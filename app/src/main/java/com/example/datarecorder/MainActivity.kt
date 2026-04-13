package com.example.datarecorder

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.datarecorder.model.AccountStatusRequest
import com.example.datarecorder.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.util.CellRangeAddress
import org.json.JSONObject
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    lateinit var db: AppDatabase
    lateinit var repository: ProjectRepository
    lateinit var appUpdateManager: AppUpdateManager
    lateinit var tvProjectName: TextView
    lateinit var btnModeToggle: Button
    var currentBuildingName: String = ""
    val packageDateMap = linkedMapOf<String, String>()
    var pendingExportLoadingFileName: String? = null
    var pendingExportLoadingBytes: ByteArray? = null
    lateinit var loadingTable: TableLayout
    lateinit var tvLoadingIronWeighbridge: TextView
    var loadingAluminumUsePackageCount: Boolean = false

    lateinit var btnProjectMenu: Button
    lateinit var btnPackageMenu: Button
    lateinit var btnMore: Button
    lateinit var tvAvatar: TextView
    lateinit var tvLoadingIronTotal: TextView

    lateinit var btnBackspace: Button
    lateinit var btnSpace: Button
    lateinit var btnNewColumn: Button
    lateinit var btnPlus: Button
    lateinit var btnMultiply: Button
    lateinit var btnBrackets: Button
    lateinit var btnNewLine: Button

    lateinit var layoutWall: GridLayout
    lateinit var layoutBeam: GridLayout
    lateinit var layoutSlab: GridLayout
    lateinit var layoutStair: GridLayout

    lateinit var standardModeContainer: LinearLayout
    lateinit var fastModeContainer: LinearLayout

    lateinit var layoutFastTail: GridLayout
    lateinit var layoutFastWidth: GridLayout
    lateinit var layoutFastLength: GridLayout
    lateinit var layoutFastCustomNumbers: GridLayout
    lateinit var layoutFastActions: LinearLayout
    lateinit var btnFastNewColumn: Button
    lateinit var btnFastNewLine: Button
    lateinit var btnFastBackspace: Button

    lateinit var layoutFastModel: GridLayout
    lateinit var loadingModeContainer: LinearLayout

    var currentModeType: ModeType = ModeType.STANDARD
    var currentLoadingTripName: String = ""
    var currentLoadingEditType: ReturnLoadingType = ReturnLoadingType.ALUMINUM
    var currentLoadingEditIndex: Int = -1
    var currentLoadingField: ReturnLoadingField = ReturnLoadingField.MATERIAL_NAME

    val loadingTripMap = linkedMapOf<String, ReturnLoadingTripData>()
    val loadingAluminumRows = mutableListOf<ReturnLoadingRow>()
    val loadingIronRows = mutableListOf<ReturnLoadingRow>()
    var vehicleInfo = VehicleInfo()


    lateinit var headerHorizontalScroll: HorizontalScrollView
    lateinit var bodyHorizontalScroll: HorizontalScrollView
    lateinit var bodyVerticalScroll: ScrollView
    lateinit var tableHeader: TableLayout
    lateinit var tableBody: TableLayout
    lateinit var tvSummaryPrimary: TextView
    lateinit var tvSummarySecondary: TextView

    var isFastMode = false
    var currentProjectId: Long = 0L
    var currentProjectName: String = "未选择"
    var currentPackageName: String = ""

    val packageStandardRowsMap = linkedMapOf<String, MutableList<StandardRow>>()
    val packageCurrentStandardRowMap = linkedMapOf<String, StandardRow>()
   val packageFastRowsMap = linkedMapOf<String, MutableList<FastRow>>()
   val packageCurrentFastRowMap = linkedMapOf<String, FastRow>()

    private val handler = Handler(Looper.getMainLooper())
    private var autoSaveRunnable: Runnable? = null
    private var historyBackupRunnable: Runnable? = null
    private val historyBackupIntervalMs = 5 * 60 * 1000L
    private val historyBackupRelativePath = "${Environment.DIRECTORY_DOCUMENTS}/铝模板统计/历史数据/"
    private val historyBackupDirNameLegacy = "铝模板统计/历史数据"

    data class BackupItem(
        val id: Long? = null,
        val fileName: String,
        val projectName: String,
        val timeMillis: Long,
        val uri: Uri? = null,
        val file: File? = null
    )

    val dfArea = DecimalFormat("0.##")
    val ioExecutor = Executors.newSingleThreadExecutor()
    var pendingExportStandardFileName: String? = null
    var pendingExportFastFileName: String? = null
    var pendingExportStandardBytes: ByteArray? = null
    var pendingExportFastBytes: ByteArray? = null

    val exportFolderPickerLauncher =

        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) {
                toast("已取消导出")
                clearPendingExportData()
                return@registerForActivityResult
            }

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                saveLastExportFolderUri(uri)
            } catch (_: Exception) {
            }

            exportCurrentProjectToSelectedFolder(uri)
        }


    val modeNameStandard = "型号统计"
    val modeNameFast = "返厂统计"

    private val wallKeys = listOf("W", "WE", "WED", "BQ", "IC", "ICA")
    private val beamKeys = listOf("B", "BS", "BC", "BP", "X", "N")
    private val slabKeys = listOf("S", "SC", "M", "MB", "SP", "Q")
    private val stairKeys = listOf("LT", "JT", "GT", "DM", "H", "P")

    val fastTailKeys = listOf("50", "45", "95", "65", "85")
    val fastWidthKeys = listOf("100", "200", "300", "400", "500")
    val fastLengthKeys = listOf("700", "900", "1100", "2700", "2745")
    val fastModelKeys = listOf("E", "F", "SP")

    val allowedInstallNoTokens = setOf(
        "A", "B", "C", "D", "E", "F", "G",
        "W", "S", "DM", "LT", "P",
        "-", "/"
    )

    private val autoSpaceModelTokens = setOf(
        "W", "WE", "WED", "BQ",
        "B", "BS", "BC", "BP",
        "S", "SC", "M", "MB", "SP",
        "LT", "JT", "GT", "DM", "IC", "ICA"
    )








    val savedStandardRows = mutableListOf<StandardRow>()
    var currentStandardRow = StandardRow()
    var currentStandardField = StandardField.INSTALL_NO
    var lastStandardField = StandardField.INSTALL_NO

    val savedFastRows = mutableListOf<FastRow>()
    var currentFastRow = FastRow()
    var currentFastNumericField = FastField.WIDTH
    var currentFastActiveField = FastField.WIDTH
    var lastFastField = FastField.WIDTH

    var editingStandardRowIndex: Int? = null
    private var editingStandardField: StandardField? = null

    private var editingFastRowIndex: Int? = null
    var editingFastField: FastField? = null
    var pendingReplaceStandardEditing = false
    var pendingReplaceFastEditing = false
    var pendingReplaceCurrentFastModel = false
    var pendingReplaceCurrentStandardModel = false
    lateinit var loadingTableHeader: TableLayout
    lateinit var loadingHeaderHorizontalScroll: HorizontalScrollView
    lateinit var loadingBodyHorizontalScroll: HorizontalScrollView
    lateinit var loadingBodyVerticalScroll: ScrollView

    private val ids = listOf(
        R.id.btnModeToggle,
        R.id.btnProjectMenu,
        R.id.btnPackageMenu,
        R.id.btnMore,
        R.id.btnBackspace,
        R.id.btnSpace,
        R.id.btnNewColumn,
        R.id.btnPlus,
        R.id.btnMultiply,
        R.id.btnBrackets,
        R.id.btnNewLine,
        R.id.btnFastNewColumn,
        R.id.btnFastNewLine,
        R.id.btnFastBackspace,
        R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
        R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
        R.id.btn00, R.id.btnDot,
        R.id.btnA, R.id.btnB, R.id.btnC, R.id.btnD,
        R.id.btnE, R.id.btnF
    )

    private val forceLogoutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra(ForceLogoutHelper.EXTRA_MESSAGE)
                ?: "账号状态异常，已退出登录"
            goLogin(message)
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.onHostResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SessionManager.isLoggedIn(this)) {
            goLogin("请先登录")
            return
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setContentView(R.layout.activity_main)

        db = AppDatabase.get(this)
        repository = ProjectRepository(db.projectDao())
        appUpdateManager = AppUpdateManager(this)

        initViews()
        initButtonTextCenter()
        initTopButtons()
        initStandardButtons()
        initStandardCustomButtons()
        initStandardGroupArea()
        initFastModeArea()
        initLoadingModeArea()
        updateAvatarView()
        updatePackageButtonText()

        currentModeType = readLastModeType()
        isFastMode = currentModeType == ModeType.FAST
        switchMode(currentModeType)


        lifecycleScope.launch {
            loadLastProjectOrCreateDefault()
            startHistoryAutoBackup()
        }

        window.decorView.postDelayed({
            appUpdateManager.checkUpdate(false)
        }, 1000)

        registerLogoutReceiver()
        checkAccountStatusNow()
    }

    override fun onDestroy() {
        autoSaveRunnable?.let { handler.removeCallbacks(it) }
        historyBackupRunnable?.let { handler.removeCallbacks(it) }

        runCatching {
            kotlinx.coroutines.runBlocking {
                saveCurrentProjectContentNow()
            }
        }

        saveHistoryBackupSnapshot()

        ioExecutor.shutdownNow()
        appUpdateManager.release()
        unregisterLogoutReceiverSafe()
        super.onDestroy()
    }



    override fun onPause() {
        runCatching {
            kotlinx.coroutines.runBlocking {
                saveCurrentProjectContentNow()
            }
        }
        saveHistoryBackupSnapshot()
        super.onPause()
    }





    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("is_fast_mode", isFastMode)
        super.onSaveInstanceState(outState)
    }


    private fun initViews() {
        tvProjectName = findViewById(R.id.tvProjectName)
        btnModeToggle = findViewById(R.id.btnModeToggle)
        loadingModeContainer = findViewById(R.id.loadingModeContainer)
        loadingTable = findViewById(R.id.loadingTable)
        tvLoadingIronWeighbridge = findViewById(R.id.tvLoadingIronWeighbridge)
        tvLoadingIronTotal = findViewById(R.id.tvLoadingIronTotal)
        loadingTableHeader = findViewById(R.id.loadingTableHeader)
        loadingHeaderHorizontalScroll = findViewById(R.id.loadingHeaderHorizontalScroll)
        loadingBodyHorizontalScroll = findViewById(R.id.loadingBodyHorizontalScroll)
        loadingBodyVerticalScroll = findViewById(R.id.loadingBodyVerticalScroll)
        loadingTable = findViewById(R.id.loadingTable)



        btnProjectMenu = findViewById(R.id.btnProjectMenu)
        btnPackageMenu = findViewById(R.id.btnPackageMenu)
        btnMore = findViewById(R.id.btnMore)
        tvAvatar = findViewById(R.id.tvAvatar)

        btnBackspace = findViewById(R.id.btnBackspace)
        btnSpace = findViewById(R.id.btnSpace)
        btnNewColumn = findViewById(R.id.btnNewColumn)
        btnPlus = findViewById(R.id.btnPlus)
        btnMultiply = findViewById(R.id.btnMultiply)
        btnBrackets = findViewById(R.id.btnBrackets)
        btnNewLine = findViewById(R.id.btnNewLine)

        layoutWall = findViewById(R.id.layoutWall)
        layoutBeam = findViewById(R.id.layoutBeam)
        layoutSlab = findViewById(R.id.layoutSlab)
        layoutStair = findViewById(R.id.layoutStair)

        standardModeContainer = findViewById(R.id.standardModeContainer)
        fastModeContainer = findViewById(R.id.fastModeContainer)

        layoutFastTail = findViewById(R.id.layoutFastTail)
        layoutFastWidth = findViewById(R.id.layoutFastWidth)
        layoutFastLength = findViewById(R.id.layoutFastLength)
        layoutFastCustomNumbers = findViewById(R.id.layoutFastCustomNumbers)
        layoutFastActions = findViewById(R.id.layoutFastActions)
        btnFastNewColumn = findViewById(R.id.btnFastNewColumn)
        btnFastNewLine = findViewById(R.id.btnFastNewLine)
        btnFastBackspace = findViewById(R.id.btnFastBackspace)

        layoutFastModel = findViewById(R.id.layoutFastModel)

        headerHorizontalScroll = findViewById(R.id.headerHorizontalScroll)
        bodyHorizontalScroll = findViewById(R.id.bodyHorizontalScroll)
        bodyVerticalScroll = findViewById(R.id.bodyVerticalScroll)
        tableHeader = findViewById(R.id.tableHeader)
        tableBody = findViewById(R.id.tableBody)
        tvSummaryPrimary = findViewById(R.id.tvSummaryPrimary)
        tvSummarySecondary = findViewById(R.id.tvSummarySecondary)

        bodyHorizontalScroll.viewTreeObserver.addOnScrollChangedListener {
            headerHorizontalScroll.scrollTo(bodyHorizontalScroll.scrollX, 0)
        }
    }

    private fun initButtonTextCenter() {
        ids.forEach { id ->
            centerButtonText(findViewById(id))
        }
    }






    private fun centerButtonText(button: Button) {
        button.gravity = Gravity.CENTER
        button.textAlignment = View.TEXT_ALIGNMENT_CENTER
        button.includeFontPadding = false
        button.setPadding(0, 0, 0, 0)
        button.isAllCaps = false
    }

    private fun initTopButtons() {
        btnProjectMenu.setOnClickListener {
            showProjectMenuPopup(it)
        }

        btnPackageMenu.setOnClickListener {
            showPackageMenuPopup(it)
        }

        btnMore.setOnClickListener {
            showMoreMenuPopup(it)
        }

        tvAvatar.setOnClickListener {
            showAccountPopup(it)
        }

        btnModeToggle.setOnClickListener {
            showModeSwitchMenu(it)
        }

    }

    private fun applyMode(fast: Boolean) {
        currentModeType = if (fast) ModeType.FAST else ModeType.STANDARD
        isFastMode = fast

        standardModeContainer.visibility = if (fast) View.GONE else View.VISIBLE
        fastModeContainer.visibility = if (fast) View.VISIBLE else View.GONE
        loadingModeContainer.visibility = View.GONE

        btnModeToggle.text = "切换模式"
        updateDisplayTable()
    }



    private fun showProjectMenuPopup(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "选择项目")
        popup.menu.add(0, 2, 1, "新建项目")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showProjectSelectDialog()
                    true
                }
                2 -> {
                    showCreateProjectDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    private fun getTodayPackageDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getPackageDate(packageName: String): String {
        return packageDateMap[packageName].orEmpty()
    }

    private fun ensurePackageDate(packageName: String) {
        if (packageName.isBlank()) return
        if (packageDateMap[packageName].isNullOrBlank()) {
            packageDateMap[packageName] = getTodayPackageDate()
        }
    }

    private fun showMoreMenuPopup(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "导出")
        popup.menu.add(0, 2, 1, "分享")
        popup.menu.add(0, 3, 2, "检查更新")
        popup.menu.add(0, 4, 3, "二维码识别")
        popup.menu.add(0, 5, 4, "NFC碰一碰")
        popup.menu.add(0, 6, 5, "历史数据")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    requestExportFolderAndExport()
                    true
                }
                2 -> {
                    shareCurrentModeProject()
                    true
                }

                3 -> {
                    appUpdateManager.checkUpdate()
                    true
                }
                4 -> {
                    showFeatureComingSoonDialog(
                        title = "二维码识别",
                        message = "你发现了一个新功能，这个功能通过摄像头连续扫描模板二维码，即可连续统计模板的安装编号及型号。但目前资金不足，人员不够，代码未写，敬请期待"
                    )
                    true
                }
                5 -> {
                    showFeatureComingSoonDialog(
                        title = "NFC碰一碰",
                        message = "你发现了一个更加强大的功能，这个功能通过手机靠近模板编码位置，即可自动统计模板的安装编号及型号。但目前资金不足，人员不够，代码未写，敬请期待"
                    )
                    true
                }
                6 -> {
                    showHistoryBackupDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showAccountPopup(anchor: View) {
        val username = SessionManager.getUsername(this).ifBlank { "未登录账号" }

        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "账号：$username")
        popup.menu.add(0, 2, 1, "退出登录")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> true
                2 -> {
                    SessionManager.clearLogin(this)
                    AccountStatusScheduler.stop(this)
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun updateAvatarView() {
        val username = SessionManager.getUsername(this).trim()
        val avatarText = when {
            username.isNotEmpty() -> username.take(1).uppercase(Locale.getDefault())
            else -> "用"
        }
        tvAvatar.text = avatarText
    }

    private fun updatePackageButtonText() {
        btnPackageMenu.text = if (currentPackageName.isBlank()) "包号" else currentPackageName
    }

    private fun ensurePackageSelected(): Boolean {
        if (currentProjectId <= 0L) {
            toast("请先选择项目")
            return false
        }
        if (currentPackageName.isBlank()) {
            toast("请先点击包号并增加包号，才能输入数据")
            return false
        }
        return true
    }

    private fun clearAllPackageMaps() {
        packageStandardRowsMap.clear()
        packageCurrentStandardRowMap.clear()
        packageFastRowsMap.clear()
        packageCurrentFastRowMap.clear()
        packageDateMap.clear()
        currentPackageName = ""
        updatePackageButtonText()
    }




    private fun saveScreenDataToCurrentPackage() {
        if (currentPackageName.isBlank()) return

        packageStandardRowsMap[currentPackageName] = savedStandardRows.map { it.copy() }.toMutableList()
        packageCurrentStandardRowMap[currentPackageName] = currentStandardRow.copy()

        packageFastRowsMap[currentPackageName] = savedFastRows.map { it.copy() }.toMutableList()
        packageCurrentFastRowMap[currentPackageName] = currentFastRow.copy()
    }

    private fun loadPackageToScreen(packageName: String) {
        currentPackageName = packageName
        updatePackageButtonText()

        savedStandardRows.clear()
        savedStandardRows.addAll(packageStandardRowsMap[packageName]?.map { it.copy() } ?: emptyList())
        currentStandardRow = packageCurrentStandardRowMap[packageName]?.copy() ?: StandardRow()
        currentStandardField = StandardField.INSTALL_NO
        lastStandardField = StandardField.INSTALL_NO

        savedFastRows.clear()
        savedFastRows.addAll(packageFastRowsMap[packageName]?.map { it.copy() } ?: emptyList())
        currentFastRow = packageCurrentFastRowMap[packageName]?.copy() ?: FastRow()
        currentFastNumericField = FastField.WIDTH
        currentFastActiveField = FastField.WIDTH
        lastFastField = FastField.WIDTH

        clearStandardEditingState()
        clearFastEditingState()
        pendingReplaceStandardEditing = false
        pendingReplaceFastEditing = false
        pendingReplaceCurrentFastModel = false
        pendingReplaceCurrentStandardModel = false

        updateDisplayTable()
    }

    private fun getAllPackageNamesInOrder(): List<String> {
        val names = linkedSetOf<String>()
        names.addAll(packageStandardRowsMap.keys)
        names.addAll(packageFastRowsMap.keys)
        return names.toList()
    }

    private fun generateNextPackageName(): String {
        val maxIndex = getAllPackageNamesInOrder()
            .mapNotNull { name ->
                Regex("""第(\d+)包""").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
            }
            .maxOrNull() ?: 0
        return "第${maxIndex + 1}包"
    }


    private fun addNewPackage() {
        if (currentProjectId <= 0L) {
            toast("请先新建或选择项目")
            return
        }

        saveScreenDataToCurrentPackage()

        val packageName = generateNextPackageName()
        packageStandardRowsMap[packageName] = mutableListOf()
        packageCurrentStandardRowMap[packageName] = StandardRow()
        packageFastRowsMap[packageName] = mutableListOf()
        packageCurrentFastRowMap[packageName] = FastRow()
        packageDateMap[packageName] = getTodayPackageDate()

        loadPackageToScreen(packageName)
        triggerAutoSave()
    }




    private fun switchPackage(packageName: String) {
        if (packageName.isBlank()) return
        if (!getAllPackageNamesInOrder().contains(packageName)) return

        saveScreenDataToCurrentPackage()
        loadPackageToScreen(packageName)
    }

    fun MainActivity.showPackageMenuPopup(anchor: View) {
        if (currentProjectId <= 0L) {
            toast("请先新建或选择项目")
            return
        }

        val popup = PopupMenu(this, anchor)
        var order = 0

        if (currentModeType == ModeType.RETURN_LOADING) {
            loadingTripMap.keys.forEachIndexed { index, tripName ->
                popup.menu.add(0, 1000 + index, order++, tripName)
            }
            popup.menu.add(0, 1, order, "增加车次")
        } else {
            getAllPackageNamesInOrder().forEachIndexed { index, packageName ->
                popup.menu.add(0, 1000 + index, order++, packageName)
            }
            popup.menu.add(0, 1, order, "增加包号")
        }

        popup.setOnMenuItemClickListener { item ->
            when {
                item.itemId == 1 -> {
                    if (currentModeType == ModeType.RETURN_LOADING) {
                        addNewLoadingTrip()
                    } else {
                        addNewPackage()
                    }
                    true
                }
                item.itemId >= 1000 -> {
                    if (currentModeType == ModeType.RETURN_LOADING) {
                        switchLoadingTrip(item.title.toString())
                    } else {
                        switchPackage(item.title.toString())
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun resetForNewProjectWithoutPackage() {
        clearAllPackageMaps()

        clearStandardEditingState()
        clearFastEditingState()
        pendingReplaceStandardEditing = false
        pendingReplaceFastEditing = false
        pendingReplaceCurrentFastModel = false
        pendingReplaceCurrentStandardModel = false

        savedStandardRows.clear()
        currentStandardRow = StandardRow()
        currentStandardField = StandardField.INSTALL_NO
        lastStandardField = StandardField.INSTALL_NO

        savedFastRows.clear()
        currentFastRow = FastRow()
        currentFastNumericField = FastField.WIDTH
        currentFastActiveField = FastField.WIDTH
        lastFastField = FastField.WIDTH

        updateDisplayTable()
    }

    // =========================
    // 标准模式
    // =========================

    fun hasStandardEditingTarget(): Boolean {
        return editingStandardRowIndex != null && editingStandardField != null
    }

    private fun initStandardButtons() {
        btnBackspace.setOnClickListener {
            deleteLastStandardInput()
        }

        btnBackspace.setOnLongClickListener {
            true
        }

        btnSpace.setOnClickListener {
            appendStandardToLastField(" ")
        }

        btnNewColumn.setOnClickListener {
            moveToNextStandardColumn()
        }

        btnPlus.setOnClickListener {
            showSymbolPopup(btnPlus)
        }

        btnMultiply.setOnClickListener {
            appendStandardToLastField("L")
        }

        btnBrackets.setOnClickListener {
            appendStandardToLastField("Y")
        }

        btnNewLine.setOnClickListener {
            finishCurrentStandardRow()
        }
    }

    private fun showSymbolPopup(anchor: View) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = dpF(14f)
                setStroke(dp(1), 0xFFE1E5F0.toInt())
            }
            elevation = dpF(6f)
        }

        val popupWindow = PopupWindow(
            container,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = dpF(8f)
        }

        val symbols = listOf("+", "-", "/", "G", "()")

        symbols.forEachIndexed { index, symbol ->
            val button = createSymbolPopupButton(symbol) {
                appendStandardToLastField(symbol)
                popupWindow.dismiss()
            }

            val lp = LinearLayout.LayoutParams(dp(48), dp(40)).apply {
                if (index > 0) marginStart = dp(6)
            }
            container.addView(button, lp)
        }

        container.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )

        val xOff = (anchor.width - container.measuredWidth) / 2
        popupWindow.showAsDropDown(anchor, xOff, dp(6))
    }

    private fun createSymbolPopupButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14f
            isAllCaps = false
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
            setTextColor(0xFF4E3D91.toInt())
            setPadding(0, 0, 0, 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xFFF3EEFF.toInt())
                cornerRadius = dpF(10f)
                setStroke(dp(1), 0xFFD7CCFF.toInt())
            }
            setOnClickListener { onClick() }
        }
    }

    private fun initStandardCustomButtons() {
        val numberMap = mapOf(
            R.id.btn0 to "0",
            R.id.btn1 to "1",
            R.id.btn2 to "2",
            R.id.btn3 to "3",
            R.id.btn4 to "4",
            R.id.btn5 to "5",
            R.id.btn6 to "6",
            R.id.btn7 to "7",
            R.id.btn8 to "8",
            R.id.btn9 to "9",
            R.id.btn00 to "00",
            R.id.btnDot to "."
        )

        numberMap.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener {
                handleStandardTokenInput(value)
            }
        }

        val letterMap = mapOf(
            R.id.btnA to "A",
            R.id.btnB to "B",
            R.id.btnC to "C",
            R.id.btnD to "D",
            R.id.btnE to "E",
            R.id.btnF to "F"
        )

        letterMap.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener {
                handleStandardTokenInput(value)
            }
        }
    }

    private fun initStandardGroupArea() {
        bindThreeColumnButtons(layoutWall, wallKeys)
        bindThreeColumnButtons(layoutBeam, beamKeys)
        bindThreeColumnButtons(layoutSlab, slabKeys)
        bindThreeColumnButtons(layoutStair, stairKeys)
    }

    private fun bindThreeColumnButtons(grid: GridLayout, keys: List<String>) {
        grid.removeAllViews()
        grid.columnCount = 3
        grid.rowCount = ((keys.size + 2) / 3)

        keys.forEachIndexed { index, key ->
            grid.addView(
                createWeightedButton(
                    text = key,
                    row = index / 3,
                    column = index % 3,
                    height = dp(24),
                    textSizeSp = 8f
                ) {
                    handleStandardTokenInput(key)
                }
            )
        }
    }





    private fun showFeatureComingSoonDialog(title: String, message: String) {
        val dialog = android.app.Dialog(this)
        dialog.setCancelable(true)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F7FB.toInt())
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dpF(20f)
                setStroke(dp(1), 0xFFE3E7F0.toInt())
            }
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF222222.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeView = TextView(this).apply {
            text = "×"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF666666.toInt())
            gravity = Gravity.CENTER
            minWidth = dp(36)
            minHeight = dp(36)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(0xFFF1F3F7.toInt())
                cornerRadius = dpF(12f)
            }
            setOnClickListener {
                dialog.dismiss()
            }
        }

        val iconView = TextView(this).apply {
            text = "✨"
            textSize = 42f
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, 0)
        }

        val hintTitle = TextView(this).apply {
            text = "敬请期待"
            textSize = 24f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF6C56B3.toInt())
            setPadding(0, dp(12), 0, 0)
        }

        val messageView = TextView(this).apply {
            text = message
            textSize = 17f
            setTextColor(0xFF333333.toInt())
            setLineSpacing(dpF(6f), 1.12f)
            setPadding(0, dp(20), 0, dp(6))
        }

        topBar.addView(titleView)
        topBar.addView(closeView)

        card.addView(topBar)
        card.addView(iconView)
        card.addView(hintTitle)
        card.addView(messageView)

        root.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        dialog.setContentView(root)
        dialog.show()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }



    fun guessLastStandardField(row: StandardRow): StandardField {
        return when {
            row.quantity.isNotEmpty() -> StandardField.QUANTITY
            row.model.isNotEmpty() -> StandardField.MODEL
            row.installNo.isNotEmpty() -> StandardField.INSTALL_NO
            else -> StandardField.INSTALL_NO
        }
    }

    fun appendModelToken(origin: String, token: String): String {
        if (token !in autoSpaceModelTokens) return origin + token

        var result = origin
        if (result.isNotEmpty() && !result.endsWith(" ")) {
            result += " "
        }
        result += token
        if (!result.endsWith(" ")) {
            result += " "
        }
        return result
    }

    fun hasFastEditingTarget(): Boolean {
        return editingFastRowIndex != null && editingFastField != null
    }

    fun showFastInputWarning() {
        Toast.makeText(
            this,
            "警告！系统检测到错误输入，请检查输入数据是否有误！",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun isFastWidthValid(value: String): Boolean {
        if (value.isBlank() || value == ".") return true
        val number = value.toDoubleOrNull() ?: return true
        return number <= 600.0
    }

    fun isFastLengthValid(value: String): Boolean {
        if (value.isBlank() || value == ".") return true
        val number = value.toDoubleOrNull() ?: return true
        return number <= 4500.0
    }

    fun isFastQuantityValid(value: String): Boolean {
        if (value.isBlank()) return true
        val number = value.toIntOrNull() ?: return true
        return number <= 500
    }

    private val fastPresetNumericTokens = setOf(
        "50", "45", "95", "65", "85",
        "100", "200", "300", "400", "500",
        "700", "900", "1100", "2700", "2745"
    )

    fun handleFastPresetReplaceOnLimit(
        field: FastField,
        token: String,
        applyValue: (String) -> Unit
    ): Boolean {
        if (token !in fastPresetNumericTokens) return false

        showFastInputWarning()
        applyValue(token)

        when (field) {
            FastField.WIDTH -> {
                currentFastNumericField = FastField.WIDTH
                currentFastActiveField = FastField.WIDTH
                lastFastField = FastField.WIDTH
            }

            FastField.LENGTH -> {
                currentFastNumericField = FastField.LENGTH
                currentFastActiveField = FastField.LENGTH
                lastFastField = FastField.LENGTH
            }

            FastField.QUANTITY -> {
                currentFastNumericField = FastField.QUANTITY
                currentFastActiveField = FastField.QUANTITY
                lastFastField = FastField.QUANTITY
            }

            FastField.MODEL -> {}
        }

        updateDisplayTable()
        triggerAutoSave()
        return true
    }

    // =========================
    // 返厂统计
    // =========================

    private fun bindFixedColumnButtons(
        grid: GridLayout,
        keys: List<String>,
        columns: Int,
        buttonHeightDp: Int,
        textSizeSp: Float,
        clickAction: (String) -> Unit
    ) {
        grid.removeAllViews()
        grid.columnCount = columns

        keys.forEachIndexed { index, key ->
            grid.addView(
                createWeightedButton(
                    text = key,
                    row = index / columns,
                    column = index % columns,
                    height = dp(buttonHeightDp),
                    textSizeSp = textSizeSp
                ) {
                    clickAction(key)
                }
            )
        }
    }

    fun bindFastCustomArea(customTextSizeSp: Float) {
        layoutFastCustomNumbers.removeAllViews()
        layoutFastCustomNumbers.columnCount = 3
        layoutFastCustomNumbers.rowCount = 4

        val numberItems = listOf(
            Triple("1", 0, 0),
            Triple("2", 0, 1),
            Triple("3", 0, 2),
            Triple("4", 1, 0),
            Triple("5", 1, 1),
            Triple("6", 1, 2),
            Triple("7", 2, 0),
            Triple("8", 2, 1),
            Triple("9", 2, 2),
            Triple("0", 3, 0),
            Triple("00", 3, 1),
            Triple(".", 3, 2)
        )

        numberItems.forEach { (text, row, col) ->
            layoutFastCustomNumbers.addView(
                createAdaptiveWeightedButton(
                    text = text,
                    row = row,
                    column = col,
                    textSizeSp = customTextSizeSp
                ) {
                    appendFastNumericToken(text)
                }
            )
        }

        btnFastNewColumn.setOnClickListener {
            moveToNextFastColumn()
        }

        btnFastNewLine.setOnClickListener {
            finishCurrentFastRow()
        }

        btnFastBackspace.setOnClickListener {
            deleteLastFastInput()
        }

        btnFastBackspace.setOnLongClickListener {
            true
        }
    }


    fun guessLastFastField(row: FastRow): FastField {
        return when {
            row.quantity.isNotEmpty() -> FastField.QUANTITY
            row.length.isNotEmpty() -> FastField.LENGTH
            row.model.isNotEmpty() -> FastField.MODEL
            row.width.isNotEmpty() -> FastField.WIDTH
            else -> FastField.WIDTH
        }
    }

    private fun guessCurrentFastNumericField(row: FastRow): FastField {
        return when {
            row.quantity.isNotEmpty() -> FastField.QUANTITY
            row.length.isNotEmpty() -> FastField.QUANTITY
            row.width.isNotEmpty() -> FastField.LENGTH
            else -> FastField.WIDTH
        }
    }

    fun resolveNextFastNumericFieldAfterModel(row: FastRow): FastField {
        return when (row.model.trim().uppercase(Locale.getDefault())) {
            "SP" -> FastField.QUANTITY
            "E", "F" -> FastField.LENGTH
            else -> {
                when {
                    row.width.isBlank() -> FastField.WIDTH
                    row.length.isBlank() -> FastField.LENGTH
                    else -> FastField.QUANTITY
                }
            }
        }
    }


    // =========================
    // 编辑状态
    // =========================

    fun clearStandardEditingState() {
        editingStandardRowIndex = null
        editingStandardField = null
        pendingReplaceStandardEditing = false
    }

    fun clearFastEditingState() {
        editingFastRowIndex = null
        editingFastField = null
        pendingReplaceFastEditing = false
    }

    private fun selectStandardSavedCell(rowIndex: Int, field: StandardField) {
        if (rowIndex !in savedStandardRows.indices) return
        editingStandardRowIndex = rowIndex
        editingStandardField = field
        currentStandardField = field
        lastStandardField = field
        pendingReplaceStandardEditing = true
        updateDisplayTable()
    }

    private fun selectFastSavedCell(rowIndex: Int, field: FastField) {
        if (rowIndex !in savedFastRows.indices) return
        editingFastRowIndex = rowIndex
        editingFastField = field
        currentFastActiveField = field
        currentFastNumericField = when (field) {
            FastField.WIDTH -> FastField.WIDTH
            FastField.MODEL -> FastField.WIDTH
            FastField.LENGTH -> FastField.LENGTH
            FastField.QUANTITY -> FastField.QUANTITY
        }
        lastFastField = field
        pendingReplaceFastEditing = true
        updateDisplayTable()
    }

    fun appendTextToEditingStandardCell(text: String): Boolean {
        val rowIndex = editingStandardRowIndex ?: return false
        val field = editingStandardField ?: return false
        val row = savedStandardRows.getOrNull(rowIndex) ?: return false

        when (field) {
            StandardField.INSTALL_NO -> {
                if (!canAppendToInstallNo(text)) {
                    toast("安装编号仅支持数字和 A/B/C/D/E/F/W/S/DM/LT/P/-")
                    return true
                }
                row.installNo = if (pendingReplaceStandardEditing) text else row.installNo + text
            }

            StandardField.MODEL -> {
                row.model = if (pendingReplaceStandardEditing) {
                    appendModelToken("", text)
                } else {
                    appendModelToken(row.model, text)
                }
            }

            StandardField.QUANTITY -> {
                if (!text.all { it.isDigit() }) {
                    if (containsLetters(text)) {
                        toast("数量内不能输入字母")
                    }
                    return true
                }
                row.quantity = if (pendingReplaceStandardEditing) text else row.quantity + text
            }
        }

        pendingReplaceStandardEditing = false
        lastStandardField = field
        updateDisplayTable()
        triggerAutoSave()
        return true
    }

    fun deleteFromEditingStandardCell(): Boolean {
        val rowIndex = editingStandardRowIndex ?: return false
        val field = editingStandardField ?: return false
        val row = savedStandardRows.getOrNull(rowIndex) ?: return false

        fun cutLast(str: String): String = if (str.isNotEmpty()) str.dropLast(1) else str

        when (field) {
            StandardField.INSTALL_NO -> row.installNo = cutLast(row.installNo)
            StandardField.MODEL -> row.model = cutLast(row.model)
            StandardField.QUANTITY -> row.quantity = cutLast(row.quantity)
        }

        pendingReplaceStandardEditing = false
        lastStandardField = field
        updateDisplayTable()
        triggerAutoSave()
        return true
    }

    fun moveEditingStandardCellToNextColumn(): Boolean {
        val field = editingStandardField ?: return false
        editingStandardField = when (field) {
            StandardField.INSTALL_NO -> StandardField.MODEL
            StandardField.MODEL -> StandardField.QUANTITY
            StandardField.QUANTITY -> StandardField.INSTALL_NO
        }
        currentStandardField = editingStandardField!!
        lastStandardField = editingStandardField!!
        updateDisplayTable()
        return true
    }

    fun appendTextToEditingFastCell(text: String): Boolean {
        val rowIndex = editingFastRowIndex ?: return false
        val field = editingFastField ?: return false
        val row = savedFastRows.getOrNull(rowIndex) ?: return false

        when (field) {
            FastField.WIDTH -> {
                val newValue = if (pendingReplaceFastEditing) text else row.width + text
                if (!isFastWidthValid(newValue)) {
                    if (text in fastPresetNumericTokens) {
                        showFastInputWarning()
                        row.width = text
                        pendingReplaceFastEditing = false
                        currentFastNumericField = FastField.WIDTH
                        currentFastActiveField = FastField.WIDTH
                        lastFastField = FastField.WIDTH
                        updateDisplayTable()
                        triggerAutoSave()
                        return true
                    }
                    showFastInputWarning()
                    return true
                }
                row.width = newValue
                currentFastNumericField = FastField.WIDTH
            }

            FastField.MODEL -> {
                row.model = text
            }

            FastField.LENGTH -> {
                val newValue = if (pendingReplaceFastEditing) text else row.length + text
                if (!isFastLengthValid(newValue)) {
                    if (text in fastPresetNumericTokens) {
                        showFastInputWarning()
                        row.length = text
                        pendingReplaceFastEditing = false
                        currentFastNumericField = FastField.LENGTH
                        currentFastActiveField = FastField.LENGTH
                        lastFastField = FastField.LENGTH
                        updateDisplayTable()
                        triggerAutoSave()
                        return true
                    }
                    showFastInputWarning()
                    return true
                }
                row.length = newValue
                currentFastNumericField = FastField.LENGTH
            }

            FastField.QUANTITY -> {
                if (text == "." || !text.all { it.isDigit() }) {
                    if (containsLetters(text)) {
                        toast("数量内不能输入字母")
                    }
                    return true
                }
                val newValue = if (pendingReplaceFastEditing) text else row.quantity + text
                if (!isFastQuantityValid(newValue)) {
                    if (text in fastPresetNumericTokens) {
                        showFastInputWarning()
                        row.quantity = text
                        pendingReplaceFastEditing = false
                        currentFastNumericField = FastField.QUANTITY
                        currentFastActiveField = FastField.QUANTITY
                        lastFastField = FastField.QUANTITY
                        updateDisplayTable()
                        triggerAutoSave()
                        return true
                    }
                    showFastInputWarning()
                    return true
                }
                row.quantity = newValue
                currentFastNumericField = FastField.QUANTITY
            }
        }

        pendingReplaceFastEditing = false
        currentFastActiveField = field
        lastFastField = field
        updateDisplayTable()
        triggerAutoSave()
        return true
    }

    fun deleteFromEditingFastCell(): Boolean {
        val rowIndex = editingFastRowIndex ?: return false
        val field = editingFastField ?: return false
        val row = savedFastRows.getOrNull(rowIndex) ?: return false

        fun cutLast(str: String): String = if (str.isNotEmpty()) str.dropLast(1) else str

        when (field) {
            FastField.WIDTH -> {
                row.width = cutLast(row.width)
                currentFastNumericField = FastField.WIDTH
            }
            FastField.MODEL -> {
                row.model = cutLast(row.model)
            }
            FastField.LENGTH -> {
                row.length = cutLast(row.length)
                currentFastNumericField = FastField.LENGTH
            }
            FastField.QUANTITY -> {
                row.quantity = cutLast(row.quantity)
                currentFastNumericField = FastField.QUANTITY
            }
        }

        pendingReplaceFastEditing = false
        currentFastActiveField = field
        lastFastField = field
        updateDisplayTable()
        triggerAutoSave()
        return true
    }

    // =========================
    // 删除行
    // =========================

    private fun showStandardRowDeleteOptions(savedRowIndex: Int?, isCurrentRow: Boolean) {
        if (savedRowIndex == null && (!isCurrentRow || currentStandardRow.isEmpty())) return

        val options = arrayOf("删除该行")

        AlertDialog.Builder(this)
            .setTitle("操作")
            .setItems(options) { _, which ->
                if (which == 0) {
                    confirmDeleteStandardRow(savedRowIndex, isCurrentRow)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDeleteStandardRow(savedRowIndex: Int?, isCurrentRow: Boolean) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除这一行吗？删除后无法恢复。")
            .setPositiveButton("删除") { _, _ ->
                if (savedRowIndex != null && savedRowIndex in savedStandardRows.indices) {
                    savedStandardRows.removeAt(savedRowIndex)

                    if (editingStandardRowIndex != null) {
                        val editingIndex = editingStandardRowIndex!!
                        when {
                            editingIndex == savedRowIndex -> clearStandardEditingState()
                            editingIndex > savedRowIndex -> editingStandardRowIndex = editingIndex - 1
                        }
                    }
                } else if (isCurrentRow) {
                    currentStandardRow = StandardRow()
                    currentStandardField = StandardField.INSTALL_NO
                    lastStandardField = StandardField.INSTALL_NO
                    clearStandardEditingState()
                }

                updateDisplayTable()
                triggerAutoSave()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showFastRowDeleteOptions(savedRowIndex: Int?, isCurrentRow: Boolean) {
        if (savedRowIndex == null && (!isCurrentRow || currentFastRow.isEmpty())) return

        val options = arrayOf("删除该行")

        AlertDialog.Builder(this)
            .setTitle("操作")
            .setItems(options) { _, which ->
                if (which == 0) {
                    confirmDeleteFastRow(savedRowIndex, isCurrentRow)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmDeleteFastRow(savedRowIndex: Int?, isCurrentRow: Boolean) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除这一行吗？删除后无法恢复。")
            .setPositiveButton("删除") { _, _ ->
                if (savedRowIndex != null && savedRowIndex in savedFastRows.indices) {
                    savedFastRows.removeAt(savedRowIndex)

                    if (editingFastRowIndex != null) {
                        val editingIndex = editingFastRowIndex!!
                        when {
                            editingIndex == savedRowIndex -> clearFastEditingState()
                            editingIndex > savedRowIndex -> editingFastRowIndex = editingIndex - 1
                        }
                    }
                } else if (isCurrentRow) {
                    currentFastRow = FastRow()
                    currentFastNumericField = FastField.WIDTH
                    currentFastActiveField = FastField.WIDTH
                    lastFastField = FastField.WIDTH
                    clearFastEditingState()
                }

                updateDisplayTable()
                triggerAutoSave()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // =========================
    // 显示区域
    // =========================

    private fun scrollDisplayToLatest() {
        bodyVerticalScroll.post {
            bodyVerticalScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun updateDisplayTable() {
        when (currentModeType) {
            ModeType.FAST -> {
                tableHeader.removeAllViews()
                tableBody.removeAllViews()
                renderFastTable()
                tvSummaryPrimary.visibility = View.VISIBLE
                tvSummarySecondary.visibility = View.VISIBLE
                tvSummaryPrimary.text = "合计面积：${formatAreaSquareMeter(calculateFastTotalArea())}"
                tvSummarySecondary.text = "合计数量：${calculateFastTotalQty()}"
            }

            ModeType.STANDARD -> {
                tableHeader.removeAllViews()
                tableBody.removeAllViews()
                renderStandardTable()
                tvSummaryPrimary.visibility = View.VISIBLE
                tvSummarySecondary.visibility = View.GONE
                tvSummaryPrimary.text = "合计数量：${calculateStandardTotalQty()}"
            }

            ModeType.RETURN_LOADING -> {
                tvSummaryPrimary.visibility = View.VISIBLE
                tvSummarySecondary.visibility = View.GONE
                renderLoadingTable()
            }
        }

        if (editingStandardRowIndex == null && editingFastRowIndex == null) {
            scrollDisplayToLatest()
        }
    }


    private fun renderStandardTable() {
        addTableHeader("序号", "安装编号", "型号", "数量")

        if (savedStandardRows.isEmpty() && currentStandardRow.isEmpty()) {
            addStandardDataRow(
                displayIndex = 1,
                data = StandardRow(),
                isCurrentRow = true,
                savedRowIndex = null
            )
            return
        }

        savedStandardRows.forEachIndexed { index, row ->
            addStandardDataRow(
                displayIndex = index + 1,
                data = row,
                isCurrentRow = false,
                savedRowIndex = index
            )
        }

        addStandardDataRow(
            displayIndex = savedStandardRows.size + 1,
            data = currentStandardRow.copy(),
            isCurrentRow = true,
            savedRowIndex = null
        )
    }
    private fun renderFastTable() {
        addTableHeader("序号", "宽度", "型号", "长度", "数量")

        if (savedFastRows.isEmpty() && currentFastRow.isEmpty()) {
            addFastDataRow(
                displayIndex = 1,
                data = FastRow(),
                isCurrentRow = true,
                savedRowIndex = null
            )
            return
        }

        savedFastRows.forEachIndexed { index, row ->
            addFastDataRow(
                displayIndex = index + 1,
                data = row,
                isCurrentRow = false,
                savedRowIndex = index
            )
        }

        addFastDataRow(
            displayIndex = savedFastRows.size + 1,
            data = currentFastRow.copy(),
            isCurrentRow = true,
            savedRowIndex = null
        )
    }


    fun addTableHeader(c1: String, c2: String, c3: String, c4: String, c5: String? = null) {
        val row = TableRow(this)
        row.addView(createTableCell(c1, true))
        row.addView(createTableCell(c2, true))
        row.addView(createTableCell(c3, true))
        row.addView(createTableCell(c4, true))
        if (c5 != null) row.addView(createTableCell(c5, true))
        tableHeader.addView(row)
    }

    private fun addStandardDataRow(
        displayIndex: Int,
        data: StandardRow,
        isCurrentRow: Boolean,
        savedRowIndex: Int? = null
    ) {
        val row = TableRow(this)

        row.addView(
            createTableCell(
                text = displayIndex.toString(),
                isHeader = false,
                highlight = isCurrentRow,
                onLongClick = {
                    showStandardRowDeleteOptions(savedRowIndex, isCurrentRow)
                    true
                }
            )
        )

        row.addView(
            createTableCell(
                text = data.installNo,
                isHeader = false,
                highlight = isCurrentRow,
                selected = if (savedRowIndex != null) {
                    editingStandardRowIndex == savedRowIndex && editingStandardField == StandardField.INSTALL_NO
                } else {
                    editingStandardRowIndex == null && currentStandardField == StandardField.INSTALL_NO
                },
                onClick = {
                    if (savedRowIndex != null) {
                        selectStandardSavedCell(savedRowIndex, StandardField.INSTALL_NO)
                    } else {
                        clearStandardEditingState()
                        pendingReplaceStandardEditing = false
                        currentStandardField = StandardField.INSTALL_NO
                        lastStandardField = StandardField.INSTALL_NO
                        updateDisplayTable()
                    }
                }
            )
        )

        row.addView(
            createTableCell(
                text = data.model,
                isHeader = false,
                highlight = isCurrentRow,
                selected = if (savedRowIndex != null) {
                    editingStandardRowIndex == savedRowIndex && editingStandardField == StandardField.MODEL
                } else {
                    editingStandardRowIndex == null && currentStandardField == StandardField.MODEL
                },
                onClick = {
                    if (savedRowIndex != null) {
                        selectStandardSavedCell(savedRowIndex, StandardField.MODEL)
                    } else {
                        clearStandardEditingState()
                        pendingReplaceStandardEditing = false
                        currentStandardField = StandardField.MODEL
                        lastStandardField = StandardField.MODEL
                        updateDisplayTable()
                    }
                }
            )
        )

        row.addView(
            createTableCell(
                text = data.quantity,
                isHeader = false,
                highlight = isCurrentRow,
                selected = if (savedRowIndex != null) {
                    editingStandardRowIndex == savedRowIndex && editingStandardField == StandardField.QUANTITY
                } else {
                    editingStandardRowIndex == null && currentStandardField == StandardField.QUANTITY
                },
                onClick = {
                    if (savedRowIndex != null) {
                        selectStandardSavedCell(savedRowIndex, StandardField.QUANTITY)
                    } else {
                        clearStandardEditingState()
                        pendingReplaceStandardEditing = false
                        currentStandardField = StandardField.QUANTITY
                        lastStandardField = StandardField.QUANTITY
                        updateDisplayTable()
                    }
                }
            )
        )

        tableBody.addView(row)
    }

    private fun addFastDataRow(
        displayIndex: Int,
        data: FastRow,
        isCurrentRow: Boolean,
        savedRowIndex: Int? = null
    ) {
        val row = TableRow(this)

        row.addView(
            createTableCell(
                text = displayIndex.toString(),
                isHeader = false,
                highlight = isCurrentRow,
                onLongClick = {
                    showFastRowDeleteOptions(savedRowIndex, isCurrentRow)
                    true
                }
            )
        )

        row.addView(
            createTableCell(
                text = data.width,
                isHeader = false,
                highlight = isCurrentRow,
                selected = if (savedRowIndex != null) {
                    editingFastRowIndex == savedRowIndex && editingFastField == FastField.WIDTH
                } else {
                    editingFastRowIndex == null && currentFastActiveField == FastField.WIDTH
                },
                onClick = {
                    if (savedRowIndex != null) {
                        selectFastSavedCell(savedRowIndex, FastField.WIDTH)
                    } else {
                        clearFastEditingState()
                        pendingReplaceFastEditing = false
                        currentFastNumericField = FastField.WIDTH
                        currentFastActiveField = FastField.WIDTH
                        lastFastField = FastField.WIDTH
                        updateDisplayTable()
                    }
                }
            )
        )

        row.addView(
            createTableCell(
                text = data.model,
                isHeader = false,
                highlight = isCurrentRow,
                selected = if (savedRowIndex != null) {
                    editingFastRowIndex == savedRowIndex && editingFastField == FastField.MODEL
                } else {
                    editingFastRowIndex == null && currentFastActiveField == FastField.MODEL
                },
                onClick = {
                    if (savedRowIndex != null) {
                        selectFastSavedCell(savedRowIndex, FastField.MODEL)
                    } else {
                        clearFastEditingState()
                        pendingReplaceFastEditing = false
                        currentFastActiveField = FastField.MODEL
                        lastFastField = FastField.MODEL
                        updateDisplayTable()
                    }
                }
            )
        )

        row.addView(
            createTableCell(
                text = data.length,
                isHeader = false,
                highlight = isCurrentRow,
                selected = if (savedRowIndex != null) {
                    editingFastRowIndex == savedRowIndex && editingFastField == FastField.LENGTH
                } else {
                    editingFastRowIndex == null && currentFastActiveField == FastField.LENGTH
                },
                onClick = {
                    if (savedRowIndex != null) {
                        selectFastSavedCell(savedRowIndex, FastField.LENGTH)
                    } else {
                        clearFastEditingState()
                        pendingReplaceFastEditing = false
                        currentFastNumericField = FastField.LENGTH
                        currentFastActiveField = FastField.LENGTH
                        lastFastField = FastField.LENGTH
                        updateDisplayTable()
                    }
                }
            )
        )

        row.addView(
            createTableCell(
                text = data.quantity,
                isHeader = false,
                highlight = isCurrentRow,
                selected = if (savedRowIndex != null) {
                    editingFastRowIndex == savedRowIndex && editingFastField == FastField.QUANTITY
                } else {
                    editingFastRowIndex == null && currentFastActiveField == FastField.QUANTITY
                },
                onClick = {
                    if (savedRowIndex != null) {
                        selectFastSavedCell(savedRowIndex, FastField.QUANTITY)
                    } else {
                        clearFastEditingState()
                        pendingReplaceFastEditing = false
                        currentFastNumericField = FastField.QUANTITY
                        currentFastActiveField = FastField.QUANTITY
                        lastFastField = FastField.QUANTITY
                        updateDisplayTable()
                    }
                }
            )
        )

        tableBody.addView(row)
    }

    fun createTableCell(
        text: String,
        isHeader: Boolean,
        highlight: Boolean = false,
        selected: Boolean = false,
        onClick: (() -> Unit)? = null,
        onLongClick: (() -> Boolean)? = null
    ): TextView {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(dp(2), dp(5), dp(2), dp(5))
            textSize = if (isHeader) 10f else 11f
            minHeight = dp(30)

            setTextColor(
                when {
                    selected -> 0xFF4E3D91.toInt()
                    isHeader -> 0xFF4A4A4A.toInt()
                    else -> 0xFF222222.toInt()
                }
            )

            if (selected) {
                setTypeface(typeface, Typeface.BOLD)
            }

            background = when {
                selected -> createCellBackground(
                    fillColor = 0xFFEDE7FF.toInt(),
                    strokeColor = 0xFF6C56B3.toInt(),
                    strokeWidthDp = 2,
                    cornerRadiusDp = 6f
                )

                isHeader -> createCellBackground(
                    fillColor = 0xFFE9EEF7.toInt(),
                    strokeColor = 0xFFD0D8E6.toInt(),
                    strokeWidthDp = 1,
                    cornerRadiusDp = 4f
                )

                highlight -> createCellBackground(
                    fillColor = 0xFFF8FBFF.toInt(),
                    strokeColor = 0xFFDCE3EF.toInt(),
                    strokeWidthDp = 1,
                    cornerRadiusDp = 4f
                )

                else -> createCellBackground(
                    fillColor = 0xFFFFFFFF.toInt(),
                    strokeColor = 0xFFDCE3EF.toInt(),
                    strokeWidthDp = 1,
                    cornerRadiusDp = 4f
                )
            }

            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(1), dp(1), dp(1), dp(1))
            }

            if (!isHeader && onClick != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }

            if (!isHeader && onLongClick != null) {
                isLongClickable = true
                setOnLongClickListener { onLongClick() }
            }
        }
    }

    private fun createCellBackground(
        fillColor: Int,
        strokeColor: Int,
        strokeWidthDp: Int,
        cornerRadiusDp: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            setStroke(dp(strokeWidthDp), strokeColor)
            cornerRadius = dpF(cornerRadiusDp)
        }
    }

    private fun calculateStandardTotalQty(): Int {
        val rows = mutableListOf<StandardRow>()
        rows.addAll(savedStandardRows)
        if (!currentStandardRow.isEmpty()) rows.add(currentStandardRow.copy())
        return rows.sumOf { it.quantity.toIntOrNull() ?: 1 }
    }

    private fun formatAreaSquareMeter(rawArea: Double): String {
        return "${dfArea.format(rawArea / 1_000_000.0)}㎡"
    }

    // =========================
// 通用按钮创建
// =========================
    fun bindAdaptiveFixedColumnButtons(
        grid: GridLayout,
        keys: List<String>,
        columns: Int,
        textSizeSp: Float,
        onClick: (String) -> Unit
    ) {
        grid.removeAllViews()
        grid.columnCount = columns
        grid.rowCount = ((keys.size + columns - 1) / columns)

        keys.forEachIndexed { index, key ->
            grid.addView(
                createAdaptiveWeightedButton(
                    text = key,
                    row = index / columns,
                    column = index % columns,
                    textSizeSp = textSizeSp
                ) {
                    onClick(key)
                }
            )
        }
    }
    private fun readLastModeType(): ModeType {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val modeName = prefs.getString("last_mode_type", ModeType.STANDARD.name)
            ?: ModeType.STANDARD.name

        return try {
            ModeType.valueOf(modeName)
        } catch (_: Exception) {
            if (prefs.getBoolean("is_fast_mode", false)) ModeType.FAST else ModeType.STANDARD
        }
    }

    private fun createAdaptiveWeightedButton(
        text: String,
        row: Int,
        column: Int,
        columnSpan: Int = 1,
        rowSpan: Int = 1,
        textSizeSp: Float,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
            setPadding(dp(1), 0, dp(1), 0)
            minWidth = 0
            minHeight = 0
            minimumWidth = 0
            minimumHeight = 0
            setBackgroundResource(R.drawable.bg_key)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            maxLines = 1

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setAutoSizeTextTypeUniformWithConfiguration(
                    6,
                    textSizeSp.toInt().coerceAtLeast(8),
                    1,
                    android.util.TypedValue.COMPLEX_UNIT_SP
                )
            }

            layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(row, rowSpan, rowSpan.toFloat()),
                GridLayout.spec(column, columnSpan, columnSpan.toFloat())
            ).apply {
                width = 0
                height = 0
                setGravity(Gravity.FILL)
                setMargins(dp(1), dp(1), dp(1), dp(1))
            }

            setOnClickListener { onClick() }
        }
    }

    fun getFastPresetButtonTextSize(): Float {
        val sw = resources.configuration.smallestScreenWidthDp
        return when {
            sw <= 320 -> 9.5f
            sw <= 360 -> 10f
            sw <= 411 -> 10.5f
            else -> 11f
        }
    }

    fun getFastCustomButtonTextSize(): Float {
        val sw = resources.configuration.smallestScreenWidthDp
        return when {
            sw <= 320 -> 12f
            sw <= 360 -> 13f
            sw <= 411 -> 14f
            else -> 15f
        }
    }

    fun getFastModelButtonTextSize(): Float {
        val sw = resources.configuration.smallestScreenWidthDp
        return when {
            sw <= 320 -> 10f
            sw <= 360 -> 10.5f
            sw <= 411 -> 11f
            else -> 12f
        }
    }

    private fun createWeightedButton(
        text: String,
        row: Int,
        column: Int,
        columnSpan: Int = 1,
        rowSpan: Int = 1,
        height: Int,
        textSizeSp: Float,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            includeFontPadding = false
            setPadding(dp(1), 0, dp(1), 0)
            minWidth = 0
            minHeight = 0
            minimumWidth = 0
            minimumHeight = 0
            setBackgroundResource(R.drawable.bg_key)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            maxLines = 1

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setAutoSizeTextTypeUniformWithConfiguration(
                    6,
                    textSizeSp.toInt().coerceAtLeast(8),
                    1,
                    android.util.TypedValue.COMPLEX_UNIT_SP
                )
            }

            layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(row, rowSpan, rowSpan.toFloat()),
                GridLayout.spec(column, columnSpan, columnSpan.toFloat())
            ).apply {
                width = 0
                this.height = height
                setGravity(Gravity.FILL)
                setMargins(dp(1), dp(1), dp(1), dp(1))
            }

            setOnClickListener { onClick() }
        }
    }

    // =========================
// 包号逻辑
// =========================




    // =========================
// 自动保存 / 历史备份
// =========================
    private fun startHistoryAutoBackup() {
        historyBackupRunnable?.let { handler.removeCallbacks(it) }
        historyBackupRunnable = object : Runnable {
            override fun run() {
                saveHistoryBackupSnapshot()
                handler.postDelayed(this, historyBackupIntervalMs)
            }
        }
        handler.postDelayed(historyBackupRunnable!!, historyBackupIntervalMs)
    }

    private fun buildHistoryBackupJson(): String {
        val json = JSONObject()
        json.put("projectId", currentProjectId)
        json.put("projectName", currentProjectName)
        json.put("savedAt", System.currentTimeMillis())
        json.put("standardContent", serializeStandardContent())
        json.put("fastContent", serializeFastContent())
        json.put("loadingContent", serializeLoadingContent())
        json.put("currentModeType", currentModeType.name)
        json.put("isFastMode", isFastMode)
        return json.toString()
    }



    private fun saveHistoryBackupSnapshot() {
        if (currentProjectId <= 0L) return

        val standardContent = serializeStandardContent()
        val fastContent = serializeFastContent()
        val loadingContent = serializeLoadingContent()

        if (standardContent.isBlank() && fastContent.isBlank() && loadingContent.isBlank()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileName = buildHistoryBackupFileName(currentProjectName)
                val bytes = buildHistoryBackupJson().toByteArray(Charsets.UTF_8)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    writeHistoryBackupByMediaStore(fileName, bytes)
                    trimHistoryBackupFilesMediaStore(currentProjectName, 5)
                } else {
                    writeHistoryBackupLegacy(fileName, bytes)
                    trimHistoryBackupFilesLegacy(currentProjectName, 5)
                }
            } catch (_: Exception) {
            }
        }
    }





    private fun buildHistoryBackupFileName(projectName: String): String {
        val safeName = projectName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${safeName}_历史数据自动备份_$time.json"
    }

    private fun writeHistoryBackupByMediaStore(fileName: String, bytes: ByteArray) {
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, "application/json")
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, historyBackupRelativePath)
            put(MediaStore.Files.FileColumns.IS_PENDING, 1)
        }

        val collection = MediaStore.Files.getContentUri("external")
        val uri = contentResolver.insert(collection, values) ?: return

        contentResolver.openOutputStream(uri)?.use { output ->
            output.write(bytes)
            output.flush()
        }

        values.clear()
        values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
        contentResolver.update(uri, values, null, null)
    }

    private fun writeHistoryBackupLegacy(fileName: String, bytes: ByteArray) {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(root, historyBackupDirNameLegacy)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, fileName)
        file.writeBytes(bytes)
    }

    private fun loadRecentHistoryBackups(projectName: String): List<BackupItem> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            loadRecentHistoryBackupsFromMediaStore(projectName)
        } else {
            loadRecentHistoryBackupsFromLegacy(projectName)
        }
    }

    private fun loadRecentHistoryBackupsFromMediaStore(projectName: String): List<BackupItem> {
        val safeName = projectName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
        val result = mutableListOf<BackupItem>()
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection =
            "${MediaStore.Files.FileColumns.RELATIVE_PATH}=? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf(
            historyBackupRelativePath,
            "${safeName}_历史数据自动备份_%.json"
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val timeIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex) ?: continue
                    val timeMillis = cursor.getLong(timeIndex) * 1000L
                    val uri = ContentUris.withAppendedId(collection, id)

                    result.add(
                        BackupItem(
                            id = id,
                            fileName = name,
                            projectName = projectName,
                            timeMillis = timeMillis,
                            uri = uri
                        )
                    )
                }
            }

        return result.sortedByDescending { it.timeMillis }.take(5)
    }

    private fun loadRecentHistoryBackupsFromLegacy(projectName: String): List<BackupItem> {
        val safeName = projectName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(root, historyBackupDirNameLegacy)
        if (!dir.exists()) return emptyList()

        val files = dir.listFiles() ?: return emptyList()

        return files
            .filter {
                it.isFile &&
                        it.name.startsWith("${safeName}_历史数据自动备份_") &&
                        it.name.endsWith(".json")
            }
            .sortedByDescending { it.lastModified() }
            .take(5)
            .map {
                BackupItem(
                    fileName = it.name,
                    projectName = projectName,
                    timeMillis = it.lastModified(),
                    file = it
                )
            }
    }

    private fun trimHistoryBackupFilesMediaStore(projectName: String, keepCount: Int) {
        val safeName = projectName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
        val all = mutableListOf<BackupItem>()
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection =
            "${MediaStore.Files.FileColumns.RELATIVE_PATH}=? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf(
            historyBackupRelativePath,
            "${safeName}_历史数据自动备份_%.json"
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val timeIndex =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex) ?: continue
                    val timeMillis = cursor.getLong(timeIndex) * 1000L
                    val uri = ContentUris.withAppendedId(collection, id)
                    all.add(
                        BackupItem(
                            id = id,
                            fileName = name,
                            projectName = projectName,
                            timeMillis = timeMillis,
                            uri = uri
                        )
                    )
                }
            }

        all.sortedByDescending { it.timeMillis }
            .drop(keepCount)
            .forEach { item ->
                item.uri?.let { contentResolver.delete(it, null, null) }
            }
    }

    private fun trimHistoryBackupFilesLegacy(projectName: String, keepCount: Int) {
        val safeName = projectName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(root, historyBackupDirNameLegacy)
        if (!dir.exists()) return

        val files = dir.listFiles() ?: return

        files
            .filter {
                it.isFile &&
                        it.name.startsWith("${safeName}_历史数据自动备份_") &&
                        it.name.endsWith(".json")
            }
            .sortedByDescending { it.lastModified() }
            .drop(keepCount)
            .forEach { it.delete() }
    }

    private fun showHistoryBackupDialog() {
        if (currentProjectId <= 0L) {
            toast("请先选择项目")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val backups = loadRecentHistoryBackups(currentProjectName)

            withContext(Dispatchers.Main) {
                if (backups.isEmpty()) {
                    toast("暂无历史数据")
                    return@withContext
                }

                val scrollView = ScrollView(this@MainActivity)
                val container = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                }
                scrollView.addView(container)

                val dialog = AlertDialog.Builder(this@MainActivity)
                    .setTitle("历史数据")
                    .setView(scrollView)
                    .setNegativeButton("关闭", null)
                    .create()

                backups.forEach { item ->
                    val row = LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(6), dp(8), dp(6), dp(8))
                    }

                    val fileNameView = TextView(this@MainActivity).apply {
                        text = item.fileName
                        textSize = 14f
                        setTextColor(0xFF222222.toInt())
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }

                    val restoreBtn = Button(this@MainActivity).apply {
                        text = "恢复"
                        textSize = 12f
                        isAllCaps = false
                        setOnClickListener {
                            confirmRestoreHistoryBackup(item)
                        }
                    }

                    row.addView(fileNameView)
                    row.addView(restoreBtn)
                    container.addView(row)
                }

                dialog.show()
            }
        }
    }

    fun getLastExportFolderUri(): Uri? {
        val value = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("last_export_folder_uri", null)
            ?: return null
        return runCatching { Uri.parse(value) }.getOrNull()
    }

    fun saveLastExportFolderUri(uri: Uri) {
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("last_export_folder_uri", uri.toString())
            .apply()
    }

    private fun confirmRestoreHistoryBackup(item: BackupItem) {
        AlertDialog.Builder(this)
            .setTitle("是否恢复历史数据")
            .setMessage("是否恢复历史数据，此操作会覆盖当前数据")
            .setNegativeButton("取消", null)
            .setPositiveButton("确认") { _, _ ->
                restoreHistoryBackup(item)
            }
            .show()
    }

    private fun restoreHistoryBackup(item: BackupItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonText = readHistoryBackupText(item)
                if (jsonText.isBlank()) {
                    withContext(Dispatchers.Main) {
                        toast("恢复失败：文件内容为空")
                    }
                    return@launch
                }

                val json = JSONObject(jsonText)
                val standardContent = json.optString("standardContent", "")
                val fastContent = json.optString("fastContent", "")
                val loadingContent = json.optString("loadingContent", "")
                val modeName = json.optString("currentModeType", ModeType.STANDARD.name)

                val backupMode = try {
                    ModeType.valueOf(modeName)
                } catch (_: Exception) {
                    if (json.optBoolean("isFastMode", false)) ModeType.FAST else ModeType.STANDARD
                }

                withContext(Dispatchers.Main) {
                    clearStandardEditingState()
                    clearFastEditingState()

                    pendingReplaceStandardEditing = false
                    pendingReplaceFastEditing = false
                    pendingReplaceCurrentFastModel = false
                    pendingReplaceCurrentStandardModel = false

                    clearAllPackageMaps()
                    deserializePackageStandardContent(standardContent)
                    deserializePackageFastContent(fastContent)
                    deserializeLoadingContent(loadingContent)

                    val allNames = linkedSetOf<String>()
                    allNames.addAll(packageStandardRowsMap.keys)
                    allNames.addAll(packageFastRowsMap.keys)

                    allNames.forEach { name ->
                        packageStandardRowsMap.putIfAbsent(name, mutableListOf())
                        packageCurrentStandardRowMap.putIfAbsent(name, StandardRow())
                        packageFastRowsMap.putIfAbsent(name, mutableListOf())
                        packageCurrentFastRowMap.putIfAbsent(name, FastRow())
                    }

                    if (allNames.isNotEmpty()) {
                        if (currentPackageName.isBlank() || !allNames.contains(currentPackageName)) {
                            currentPackageName = allNames.first()
                        }
                        loadPackageToScreen(currentPackageName)
                    }

                    if (loadingTripMap.isNotEmpty()) {
                        if (currentLoadingTripName.isBlank() || !loadingTripMap.containsKey(currentLoadingTripName)) {
                            currentLoadingTripName = loadingTripMap.keys.first()
                        }
                        val trip = loadingTripMap[currentLoadingTripName]
                        loadingAluminumRows.clear()
                        loadingAluminumRows.addAll(trip?.aluminumRows ?: emptyList())
                        loadingIronRows.clear()
                        loadingIronRows.addAll(trip?.ironRows ?: emptyList())
                        vehicleInfo = trip?.vehicleInfo ?: VehicleInfo()
                    }

                    when (backupMode) {
                        ModeType.STANDARD -> switchMode(ModeType.STANDARD)
                        ModeType.FAST -> switchMode(ModeType.FAST)
                        ModeType.RETURN_LOADING -> switchMode(ModeType.RETURN_LOADING)
                    }

                    updatePackageButtonText()
                    updateDisplayTable()
                    renderLoadingTable()

                    runCatching {
                        lifecycleScope.launch(Dispatchers.IO) {
                            saveCurrentProjectContentNow()
                        }
                    }

                    toast("历史数据已恢复")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toast("恢复失败：${e.message}")
                }
            }
        }
    }




    private suspend fun saveCurrentProjectContentNow() {
        if (currentProjectId <= 0L) return

        saveCurrentPackageToMemoryForSave()
        saveLoadingScreenToCurrentTrip()

        repository.updateProject(
            id = currentProjectId,
            name = currentProjectName,
            buildingName = currentBuildingName,
            standardContent = serializeStandardContent(),
            fastContent = serializeFastContent(),
            loadingContent = serializeLoadingContent()
        )
    }




    private fun readHistoryBackupText(item: BackupItem): String {
        return when {
            item.uri != null -> {
                contentResolver.openInputStream(item.uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
            }

            item.file != null && item.file.exists() -> {
                item.file.readText(Charsets.UTF_8)
            }

            else -> ""
        }
    }

    fun triggerAutoSave() {
        autoSaveRunnable?.let { handler.removeCallbacks(it) }
        autoSaveRunnable = Runnable { saveCurrentProjectContent() }
        handler.postDelayed(autoSaveRunnable!!, 250)
    }

    fun MainActivity.saveCurrentProjectContent() {
        if (currentProjectId <= 0L) return

        lifecycleScope.launch(Dispatchers.IO) {
            saveCurrentProjectContentNow()
        }
    }








    fun saveCurrentPackageToMemoryForSave() {
        saveScreenDataToCurrentPackage()
    }


    fun rebuildPackageMapsFromProject(project: ProjectEntity) {
        clearAllPackageMaps()

        deserializePackageStandardContent(project.standardContent)
        deserializePackageFastContent(project.fastContent)

        val allNames = linkedSetOf<String>()
        allNames.addAll(packageStandardRowsMap.keys)
        allNames.addAll(packageFastRowsMap.keys)

        allNames.forEach { name ->
            packageStandardRowsMap.putIfAbsent(name, mutableListOf())
            packageCurrentStandardRowMap.putIfAbsent(name, StandardRow())
            packageFastRowsMap.putIfAbsent(name, mutableListOf())
            packageCurrentFastRowMap.putIfAbsent(name, FastRow())
        }

        if (currentPackageName.isBlank() && allNames.isNotEmpty()) {
            currentPackageName = allNames.first()
        }

        if (currentPackageName.isBlank()) {
            resetForNewProjectWithoutPackage()
        } else {
            loadPackageToScreen(currentPackageName)
        }
    }

    // =========================
// 项目
// =========================
    private suspend fun loadLastProjectOrCreateDefault() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastProjectId = prefs.getLong("last_project_id", -1L)

        val allProjects = withContext(Dispatchers.IO) { repository.getAllProjects() }

        val target = allProjects.firstOrNull { it.id == lastProjectId } ?: allProjects.firstOrNull()

        if (target != null) {
            switchProject(target)
            return
        }

        val newId = withContext(Dispatchers.IO) {
            repository.createProject("默认项目", "")
        }

        val newProject = withContext(Dispatchers.IO) {
            repository.getProjectById(newId)
        }

        if (newProject != null) {
            switchProject(newProject)
        }
    }



    private fun showCreateProjectDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, 0)
        }

        val inputProjectName = EditText(this).apply {
            hint = "请输入项目名称"
            isSingleLine = true
        }

        val inputBuildingName = EditText(this).apply {
            hint = "请输入楼栋号，如：1号楼"
            isSingleLine = true
        }

        container.addView(
            inputProjectName,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val topMargin = (12 * resources.displayMetrics.density).toInt()
        container.addView(
            inputBuildingName,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                this.topMargin = topMargin
            }
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle("新建项目")
            .setView(container)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = inputProjectName.text.toString().trim()
                val buildingName = inputBuildingName.text.toString().trim()

                if (name.isEmpty()) {
                    toast("项目名称不能为空")
                    return@setOnClickListener
                }

                if (buildingName.isEmpty()) {
                    toast("楼栋号不能为空")
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val exists = withContext(Dispatchers.IO) {
                        repository.getAllProjects().any { it.name == name && it.buildingName == buildingName }
                    }

                    if (exists) {
                        toast("项目已存在")
                    } else {
                        hideKeyboard(inputBuildingName)
                        dialog.dismiss()
                        createProjectInternal(name, buildingName)
                    }
                }
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                hideKeyboard(inputBuildingName)
                dialog.dismiss()
            }

            inputProjectName.requestFocus()
        }

        dialog.setOnDismissListener {
            hideKeyboard(inputBuildingName)
            refreshMainLayoutAfterKeyboard()
        }

        dialog.show()
    }



    private suspend fun createProjectInternal(name: String, buildingName: String) {
        val newId = withContext(Dispatchers.IO) {
            repository.createProject(name, buildingName)
        }

        val project = withContext(Dispatchers.IO) {
            repository.getAllProjects().firstOrNull { it.id == newId }
        }

        if (project != null) {
            switchProject(project)
            currentBuildingName = buildingName
            resetForNewProjectWithoutPackage()
            saveCurrentProjectContent()
            toast("已创建项目：$name")
        }
    }





    fun showProjectSelectDialog() {
        lifecycleScope.launch {
            val projects = withContext(Dispatchers.IO) { repository.getAllProjects() }

            if (projects.isEmpty()) {
                toast("暂无项目")
                return@launch
            }

            val scrollView = ScrollView(this@MainActivity)
            val container = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(8), dp(8), dp(8))
            }
            scrollView.addView(container)

            val dialog = AlertDialog.Builder(this@MainActivity)
                .setTitle("选择项目")
                .setView(scrollView)
                .setNegativeButton("关闭", null)
                .create()

            projects.forEach { project ->
                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(6), dp(6), dp(6), dp(6))
                }

                val nameView = TextView(this@MainActivity).apply {
                    text = "${project.name}（${project.buildingName}）"
                    textSize = 16f
                    setTextColor(0xFF222222.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    setPadding(dp(4), dp(8), dp(4), dp(8))
                    setOnClickListener {
                        switchProject(project)
                        dialog.dismiss()
                    }
                }

                val deleteBtn = Button(this@MainActivity).apply {
                    text = "删除"
                    textSize = 12f
                    isAllCaps = false
                    minWidth = 0
                    minimumWidth = 0
                    setPadding(dp(10), dp(4), dp(10), dp(4))
                    setOnClickListener {
                        confirmDeleteProject(project, dialog)
                    }
                }

                row.addView(nameView)
                row.addView(deleteBtn)
                container.addView(row)
            }

            dialog.show()
        }
    }


    private fun confirmDeleteProject(project: ProjectEntity, parentDialog: AlertDialog) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除项目“${project.name}”吗？\n删除后无法恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    deleteProjectInternal(project)
                    parentDialog.dismiss()
                    showProjectSelectDialog()
                }
            }
            .show()
    }

    private suspend fun deleteProjectInternal(project: ProjectEntity) {
        withContext(Dispatchers.IO) {
            repository.deleteProject(project)
        }

        val remainProjects = withContext(Dispatchers.IO) {
            repository.getAllProjects()
        }

        if (project.id == currentProjectId) {
            val nextProject = remainProjects.firstOrNull()
            if (nextProject != null) {
                switchProject(nextProject)
            } else {
                val newId = withContext(Dispatchers.IO) {
                    repository.createProject("默认项目", "")
                }
                val newProject = withContext(Dispatchers.IO) {
                    repository.getAllProjects().firstOrNull { it.id == newId }
                }
                if (newProject != null) {
                    switchProject(newProject)
                }
            }
        }

        toast("项目已删除")
    }


    private fun hideKeyboard(view: View? = currentFocus) {
        val targetView = view ?: window.decorView
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(targetView.windowToken, 0)
        targetView.clearFocus()
    }

    private fun refreshMainLayoutAfterKeyboard() {
        window.decorView.post {
            standardModeContainer.requestLayout()
            fastModeContainer.requestLayout()
            tableHeader.requestLayout()
            tableBody.requestLayout()
            updateDisplayTable()
        }
    }




    // =========================
// 导出 / 分享
// =========================



    fun MainActivity.safeMerge(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        firstRow: Int,
        lastRow: Int,
        firstCol: Int,
        lastCol: Int
    ) {
        if (firstRow > lastRow) return
        if (firstCol > lastCol) return
        if (firstRow == lastRow && firstCol == lastCol) return
        sheet.addMergedRegion(CellRangeAddress(firstRow, lastRow, firstCol, lastCol))
    }



    private var logoutReceiverRegistered = false



    private fun registerLogoutReceiver() {
        if (logoutReceiverRegistered) return

        val filter = IntentFilter(ForceLogoutHelper.ACTION_FORCE_LOGOUT)

        ContextCompat.registerReceiver(
            this,
            forceLogoutReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        logoutReceiverRegistered = true
    }





    private fun unregisterLogoutReceiverSafe() {
        if (!logoutReceiverRegistered) return
        try {
            unregisterReceiver(forceLogoutReceiver)
        } catch (_: Exception) {
        } finally {
            logoutReceiverRegistered = false
        }
    }








    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }



    private fun checkAccountStatusNow() {
        val username = SessionManager.getUsername(this)
        if (username.isBlank()) {
            goLogin("登录状态已失效")
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.checkAccountStatus(
                    AccountStatusRequest(username = username)
                )

                val data = response.data
                if (response.code == 200 && data != null) {
                    if (!data.valid) {
                        ForceLogoutHelper.logout(
                            this@MainActivity,
                            data.message.ifBlank { "账号状态异常，已退出登录" }
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
    }


    private fun goLogin(message: String) {
        SessionManager.saveLogoutReason(this, message)
        SessionManager.clearLogin(this)
        AccountStatusScheduler.stop(this)

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    fun dpF(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}

