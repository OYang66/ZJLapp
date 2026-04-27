package com.example.datarecorder

class ProjectRepository(private val dao: ProjectDao) {

    suspend fun createProject(
        name: String,
        buildingName: String
    ): Long {
        return dao.insert(
            ProjectEntity(
                name = name,
                buildingName = buildingName,
                standardContent = "",
                fastContent = "",
                loadingContent = "",
                qualityContent = ""
            )
        )
    }

    suspend fun updateProject(
        id: Long,
        name: String,
        buildingName: String,
        standardContent: String,
        fastContent: String,
        loadingContent: String,
        qualityContent: String
    ) {
        dao.update(
            ProjectEntity(
                id = id,
                name = name,
                buildingName = buildingName,
                standardContent = standardContent,
                fastContent = fastContent,
                loadingContent = loadingContent,
                qualityContent = qualityContent
            )
        )
    }

    suspend fun getAllProjects(): List<ProjectEntity> {
        return dao.getAllProjects()
    }

    suspend fun getProjectById(id: Long): ProjectEntity? {
        return dao.getById(id)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        dao.delete(project)
    }
}
