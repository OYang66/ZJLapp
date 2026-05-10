package com.example.datarecorder

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.datarecorder.network.RetrofitClient
import com.iflytek.sparkchain.core.asr.ASR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.util.CellRangeAddress
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors


data class BackupItem(
		val id: Long? = null,
		val fileName: String,
		val projectName: String,
		val timeMillis: Long,
		val uri: Uri? = null,
		val file: File? = null
)

class MainActivity : AppCompatActivity() {

	var loadingAluminumWeightMode: LoadingWeightMode = LoadingWeightMode.UNSELECTED
	var loadingIronWeightMode: LoadingWeightMode = LoadingWeightMode.UNSELECTED

	var logoutReceiverRegistered = false
	val forceLogoutReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			val message = intent?.getStringExtra(ForceLogoutHelper.EXTRA_MESSAGE)
				?: "账号状态异常，已退出登录"
			goLogin(message)
		}
	}

	val standardInstallCellMap = linkedMapOf<Int, TextView>()
	val standardModelCellMap = linkedMapOf<Int, TextView>()
	val standardQuantityCellMap = linkedMapOf<Int, TextView>()

	val fastWidthCellMap = linkedMapOf<Int, TextView>()
	val fastModelCellMap = linkedMapOf<Int, TextView>()
	val fastLengthCellMap = linkedMapOf<Int, TextView>()
	val fastQuantityCellMap = linkedMapOf<Int, TextView>()

	var currentStandardInstallCell: TextView? = null
	var currentStandardModelCell: TextView? = null
	var currentStandardQuantityCell: TextView? = null

	var currentFastWidthCell: TextView? = null
	var currentFastModelCell: TextView? = null
	var currentFastLengthCell: TextView? = null
	var currentFastQuantityCell: TextView? = null
	val standardRowViewMap = linkedMapOf<Int, TableRow>()
	var currentStandardRowView: TableRow? = null

	val fastRowViewMap = linkedMapOf<Int, TableRow>()
	var currentFastRowView: TableRow? = null

	lateinit var btnBuildingMenu: Button

	val buildingStandardContentMap = linkedMapOf<String, String>()
	val buildingFastContentMap = linkedMapOf<String, String>()
	val buildingLoadingContentMap = linkedMapOf<String, String>()
	val buildingQualityContentMap = linkedMapOf<String, String>()

	var currentModeType: ModeType = ModeType.STANDARD
	var currentLoadingTripName: String = ""

	var pendingQualityPhotoRowIndex: Int? = null
	var pendingQualityPhotoUri: Uri? = null

	var currentQualityFloorLabel: String = "1"


	val qualityRows = mutableListOf<QualityFeedbackRow>()
	var currentQualityRow = QualityFeedbackRow()

	var editingQualityRowIndex: Int? = null
	var editingQualityField: QualityFeedbackField = QualityFeedbackField.MATERIAL_TYPE

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

	var pendingExportQualityFileName: String? = null
	var pendingExportQualityBytes: ByteArray? = null

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

	lateinit var layoutQualityWall: GridLayout
	lateinit var layoutQualityBeam: GridLayout
	lateinit var layoutQualitySlab: GridLayout
	lateinit var layoutQualityStair: GridLayout

	lateinit var standardModeContainer: LinearLayout
	lateinit var fastModeContainer: LinearLayout
	lateinit var qualityModeContainer: LinearLayout

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


	var currentLoadingEditType: ReturnLoadingType = ReturnLoadingType.ALUMINUM
	var currentLoadingEditIndex: Int = -1
	var currentLoadingField: ReturnLoadingField = ReturnLoadingField.MATERIAL_NAME

	val loadingTripMap = linkedMapOf<String, ReturnLoadingTripData>()
	val loadingAluminumRows = mutableListOf<ReturnLoadingRow>()
	val loadingIronRows = mutableListOf<ReturnLoadingRow>()
	var vehicleInfo = VehicleInfo()


	var pendingCameraImageUri: Uri? = null


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
	var fastSparkAsr: ASR? = null
	var fastSparkInitialized = false
	var fastSparkSessionIndex = 0
	val fastSparkTextBuilder = StringBuilder()
	var fastSparkStopRunnable: Runnable? = null
	var fastVoiceRecorder: FastVoiceAudioRecorder? = null
	var fastVoiceListening = false
	var fastVoiceWaitingResult = false
	var fastVoiceIgnoreResult = false
	var fastVoiceTouchActive = false
	var fastVoiceAwaitingPermission = false
	var fastVoicePressStartedAt = 0L
	var fastVoiceHoldDialog: Dialog? = null
	val fastVoiceHoldAnimators = mutableListOf<Animator>()
	var fastVoiceHoldButton: View? = null
	var fastVoiceButtonOverlayHost: ViewGroup? = null
	val fastVoiceButtonEffectAnimators = mutableListOf<ValueAnimator>()
	val fastVoiceButtonEffectDrawables = mutableListOf<Drawable>()

	private val handler = Handler(Looper.getMainLooper())
	private var autoSaveRunnable: Runnable? = null
	private var historyBackupRunnable: Runnable? = null
	private var onlineActiveRunnable: Runnable? = null
	private val historyBackupIntervalMs = 5 * 60 * 1000L
	val historyBackupRelativePath = "${Environment.DIRECTORY_DOCUMENTS}/铝模板统计/历史数据/"
	val historyBackupDirNameLegacy = "铝模板统计/历史数据"


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

	val qualityPhotoCameraLauncher =
		registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
			if (!success) return@registerForActivityResult

			val uri = pendingQualityPhotoUri ?: return@registerForActivityResult
			addPhotoToQualityRow(
				rowIndex = pendingQualityPhotoRowIndex,
				uri = uri
			)
		}

	val qualityPhotoGalleryLauncher =
		registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
			if (uri == null) return@registerForActivityResult

			try {
				contentResolver.takePersistableUriPermission(
					uri,
					Intent.FLAG_GRANT_READ_URI_PERMISSION
				)
			} catch (_: Exception) {
			}

			addPhotoToQualityRow(
				rowIndex = pendingQualityPhotoRowIndex,
				uri = uri
			)
		}

	val fastVoicePermissionLauncher =
		registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
			if (granted) {
				fastVoiceAwaitingPermission = false
				toast("已授予录音权限，请重新长按说话")
			} else {
				fastVoiceAwaitingPermission = false
				toast("未授予录音权限，无法使用语音识别")
			}
		}

	val modeNameStandard = "型号统计"
	val modeNameFast = "返厂统计"

	private val wallKeys = listOf("W", "WE", "WED", "BQ", "IC", "ICA")
	private val beamKeys = listOf("B", "BS", "BC", "BP", "X", "N")
	private val slabKeys = listOf("S", "SC", "M", "MB", "SP", "Q")
	private val stairKeys = listOf("LT", "JT", "GT", "DM", "H", "P")

	val qualityWallKeys = listOf("W", "WE", "WED", "BQ", "IC", "ICA")
	val qualityBeamKeys = listOf("B", "BS", "BC", "BP", "X", "N")
	val qualitySlabKeys = listOf("S", "SC", "M", "MB", "SP", "Q")
	val qualityStairKeys = listOf("LT", "JT", "GT", "DM", "H", "P")

	val fastTailKeys = listOf("50", "45", "95", "65", "85")
	val fastWidthKeys = listOf("100", "200", "300", "400", "500")
	val fastLengthKeys = listOf("700", "900", "1100", "2700", "2745")
	val fastModelKeys = listOf("E", "F", "SP")

	val allowedInstallNoTokens = setOf(
		"A", "B", "C", "D", "E", "F", "G", "K",
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

	var editingFastRowIndex: Int? = null
	var editingFastField: FastField? = null

	var editingStandardRowIndex: Int? = null
	var editingStandardField: StandardField? = null

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
		R.id.btnE, R.id.btnF,

		R.id.btnQualityPlus,
		R.id.btnQualityMultiply,
		R.id.btnQualityBrackets,
		R.id.btnQualitySpace,
		R.id.btnQualityBackspace,
		R.id.btnQualityA, R.id.btnQualityB, R.id.btnQualityC,
		R.id.btnQualityD, R.id.btnQualityE, R.id.btnQualityF,
		R.id.btnQuality0, R.id.btnQuality1, R.id.btnQuality2, R.id.btnQuality3,
		R.id.btnQuality4, R.id.btnQuality5, R.id.btnQuality6, R.id.btnQuality7,
		R.id.btnQuality8, R.id.btnQuality9, R.id.btnQuality00, R.id.btnQualityDot,
		R.id.btnQualityNextColumn, R.id.btnQualityNextRow

	)

	override fun onResume() {
		super.onResume()
		appUpdateManager.onHostResume()
		checkAccountStatusNow()
		reportOnlineActive()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		RetrofitClient.init(applicationContext)

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
		initQualityModeArea()
		initQualityInputButtons()
		registerLogoutReceiver()
		checkAccountStatusNow()
		reportOnlineActive()
		startOnlineActiveHeartbeat()
		AccountStatusScheduler.start(this)

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

	}

	override fun onDestroy() {

		autoSaveRunnable?.let {
			handler.removeCallbacks(it)
		}

		historyBackupRunnable?.let {
			handler.removeCallbacks(it)
		}
		onlineActiveRunnable?.let {
			handler.removeCallbacks(it)
		}

		runCatching {
			saveCurrentProjectContent()
		}

		saveHistoryBackupSnapshot()

		unregisterLogoutReceiverSafe()

		appUpdateManager.release()
		releaseFastSparkVoice()

		ioExecutor.shutdown()

		super.onDestroy()
	}

	override fun onPause() {
		runCatching {
			saveCurrentProjectContent()
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
		btnBuildingMenu = findViewById(R.id.btnBuildingMenu)
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
		layoutQualityWall = findViewById(R.id.layoutQualityWall)
		layoutQualityBeam = findViewById(R.id.layoutQualityBeam)
		layoutQualitySlab = findViewById(R.id.layoutQualitySlab)
		layoutQualityStair = findViewById(R.id.layoutQualityStair)

		standardModeContainer = findViewById(R.id.standardModeContainer)
		fastModeContainer = findViewById(R.id.fastModeContainer)
		qualityModeContainer = findViewById(R.id.qualityModeContainer)

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
		button.setPadding(8, 0, 8, 0)
		button.isAllCaps = false
		button.maxLines = 1
		button.ellipsize = android.text.TextUtils.TruncateAt.END
	}

	private fun initTopButtons() {
		btnProjectMenu.setOnClickListener {
			showProjectMenuPopup(it)
		}

		btnBuildingMenu.setOnClickListener {
			showBuildingMenuPopup(it)
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
		qualityModeContainer.visibility = View.GONE

		btnModeToggle.text = "切换模式"
		updateDisplayTable()
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

	private fun showAccountPopup(anchor: View) {
		val username = SessionManager.getUsername(this).ifBlank { "未登录账号" }
		val onlineText = when (SessionManager.getOnlineStatus(this)) {
			"1" -> "在线"
			else -> "离线"
		}
		val lastActiveText = SessionManager.getLastActiveTime(this).ifBlank { "暂无" }

		showAccountCardPopup(
			anchor = anchor,
			username = username,
			onlineText = onlineText,
			lastActiveText = lastActiveText,
			onOpenBackend = {
				val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yxff.work/"))
				startActivity(intent)
			},
			onLogout = {
				SessionManager.clearLogin(this)
				AccountStatusScheduler.stop(this)
				startActivity(Intent(this, LoginActivity::class.java))
				finish()
			}
		)
	}


	private fun startOnlineActiveHeartbeat() {
		onlineActiveRunnable?.let { handler.removeCallbacks(it) }
		onlineActiveRunnable = object : Runnable {
			override fun run() {
				if (!isFinishing && !isDestroyed) {
					reportOnlineActive()
					handler.postDelayed(this, 5 * 60 * 1000L)
				}
			}
		}
		handler.postDelayed(onlineActiveRunnable!!, 5 * 60 * 1000L)
	}

	private fun updateAvatarView() {
		val username = SessionManager.getUsername(this).trim()
		val avatarText = when {
			username.isNotEmpty() -> username.take(1).uppercase(Locale.getDefault())
			else -> "用"
		}
		tvAvatar.text = avatarText
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
		triggerAutoSaveDebounced()
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
			showSymbolPopup(btnPlus) { symbol ->
				appendStandardToLastField(symbol)
			}
		}


		btnMultiply.setOnClickListener {
			appendStandardToLastField("K")
		}

		btnBrackets.setOnClickListener {
			appendStandardToLastField("Y")
		}

		btnNewLine.setOnClickListener {
			finishCurrentStandardRow()
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
		val textSize = getStandardGroupButtonTextSize()

		bindAdaptiveFixedColumnButtons(layoutWall, wallKeys, 4, textSize) { value ->
			handleStandardTokenInput(value)
		}
		bindAdaptiveFixedColumnButtons(layoutBeam, beamKeys, 4, textSize) { value ->
			handleStandardTokenInput(value)
		}
		bindAdaptiveFixedColumnButtons(layoutSlab, slabKeys, 4, textSize) { value ->
			handleStandardTokenInput(value)
		}
		bindAdaptiveFixedColumnButtons(layoutStair, stairKeys, 4, textSize) { value ->
			handleStandardTokenInput(value)
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

	val fastPresetNumericTokens = setOf(
		"50", "45", "95", "65", "85",
		"100", "200", "300", "400", "500",
		"700", "900", "1100", "2700", "2745"
	)


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


	private fun guessCurrentFastNumericField(row: FastRow): FastField {
		return when {
			row.quantity.isNotEmpty() -> FastField.QUANTITY
			row.length.isNotEmpty() -> FastField.QUANTITY
			row.width.isNotEmpty() -> FastField.LENGTH
			else -> FastField.WIDTH
		}
	}


	fun containsLetters(text: String): Boolean {
		return text.any { it.isLetter() }
	}


	fun confirmDeleteStandardRow(savedRowIndex: Int?, isCurrentRow: Boolean) {
		showConfirmCardDialog(
			title = "确认删除",
			message = "确定删除这一行吗？删除后无法恢复。",
			confirmText = "删除",
			dangerMessage = true
		) {
			if (savedRowIndex != null && savedRowIndex in savedStandardRows.indices) {
				savedStandardRows.removeAt(savedRowIndex)

				if (editingStandardRowIndex != null) {
					val editingIndex = editingStandardRowIndex!!
					when {
						editingIndex == savedRowIndex -> clearStandardEditingState()
						editingIndex > savedRowIndex -> editingStandardRowIndex =
							editingIndex - 1
					}
				}
			} else if (isCurrentRow) {
				currentStandardRow = StandardRow()
				currentStandardField = StandardField.INSTALL_NO
				lastStandardField = StandardField.INSTALL_NO
				clearStandardEditingState()
			}

			updateDisplayTable()
			triggerAutoSaveDebounced()
		}
	}

	fun confirmDeleteFastRow(savedRowIndex: Int?, isCurrentRow: Boolean) {
		showConfirmCardDialog(
			title = "确认删除",
			message = "确定删除这一行吗？删除后无法恢复。",
			confirmText = "删除",
			dangerMessage = true
		) {
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
			triggerAutoSaveDebounced()
		}
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
				tvSummaryPrimary.text =
					"合计面积：${formatAreaSquareMeter(calculateFastTotalArea())}"
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

			ModeType.QUALITY_FEEDBACK -> {
				tableHeader.removeAllViews()
				tableBody.removeAllViews()
				renderQualityFeedbackTable()
				tvSummaryPrimary.visibility = View.GONE
				tvSummarySecondary.visibility = View.GONE
			}
		}

		if (editingStandardRowIndex == null && editingFastRowIndex == null) {
			scrollDisplayToLatest()
		}
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

	fun formatAreaSquareMeter(rawArea: Double): String {
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
			if (prefs.getBoolean("is_fast_mode", false)) {
				ModeType.FAST
			} else {
				ModeType.STANDARD
			}
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


	fun buildHistoryBackupFileName(projectName: String): String {
		val safeName = projectName.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
		val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
		return "${safeName}_历史数据自动备份_$time.json"
	}

	fun writeHistoryBackupByMediaStore(fileName: String, bytes: ByteArray) {
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

	fun writeHistoryBackupLegacy(fileName: String, bytes: ByteArray) {
		val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
		val dir = File(root, historyBackupDirNameLegacy)
		if (!dir.exists()) {
			dir.mkdirs()
		}
		val file = File(dir, fileName)
		file.writeBytes(bytes)
	}


	fun trimHistoryBackupFilesMediaStore(projectName: String, keepCount: Int) {
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
				val nameIndex =
					cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
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

	fun trimHistoryBackupFilesLegacy(projectName: String, keepCount: Int) {
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

	fun confirmRestoreHistoryBackup(item: BackupItem) {
		showConfirmCardDialog(
			title = "是否恢复历史数据",
			message = "是否恢复历史数据，此操作会覆盖当前数据。",
			confirmText = "确认",
			dangerMessage = true
		) {
			restoreHistoryBackup(item)
		}
	}


	fun MainActivity.snapshotCurrentProjectState() {
		saveCurrentPackageToMemoryForSave()
		saveCurrentBuildingScopeToMemory()
	}

	suspend fun MainActivity.saveCurrentProjectContentNow() {
		if (currentProjectId <= 0L) return

		snapshotCurrentProjectState()

		repository.updateProject(
			id = currentProjectId,
			name = currentProjectName,
			buildingName = currentBuildingName,
			standardContent = serializeProjectStandardBuildingsContent(),
			fastContent = serializeProjectFastBuildingsContent(),
			loadingContent = serializeProjectLoadingBuildingsContent(),
			qualityContent = serializeProjectQualityBuildingsContent()
		)
	}


	fun triggerAutoSaveDebounced() {

		autoSaveRunnable?.let {
			handler.removeCallbacks(it)
		}

		autoSaveRunnable = Runnable {
			saveCurrentProjectContent()
		}

		handler.postDelayed(autoSaveRunnable!!, 1500)
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


	// =========================
// 项目
// =========================
	private suspend fun loadLastProjectOrCreateDefault() {
		val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
		val lastProjectId = prefs.getLong("last_project_id", -1L)

		val allProjects = withContext(Dispatchers.IO) { repository.getAllProjects() }

		val target = allProjects.firstOrNull { it.id == lastProjectId } ?: allProjects.firstOrNull()

		if (target != null) {
			switchProjectById(target.id)
			return
		}

		val newId = withContext(Dispatchers.IO) {
			repository.createProject("默认项目", "1#")
		}

		switchProjectById(newId)
	}


	fun MainActivity.getStandardGroupButtonTextSize(): Float {
		val widthDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
		return when {
			widthDp <= 360f -> 9.5f
			widthDp <= 400f -> 10.5f
			widthDp <= 480f -> 11f
			else -> 12f
		}
	}


	fun hideKeyboard(view: View? = currentFocus) {
		val targetView = view ?: window.decorView
		val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
		imm.hideSoftInputFromWindow(targetView.windowToken, 0)
		targetView.clearFocus()
	}

	fun refreshMainLayoutAfterKeyboard() {
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


	private fun escapeHtml(text: String): String {
		return text
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;")
	}


}

