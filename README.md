# RestLib
REST library based on OkHttp3 and Gson

Connect to project:
1. Add the JitPack repository to project-level build.gradle file
<pre>
repositories {
	...
	maven { url 'https://jitpack.io' }
}
</pre>
2. Add the dependency to module-level gradle file
<pre>
dependencies {
	...
	implementation 'com.github.snk002:RestLib:Tag'
}
</pre>
Sample usage:
<pre>
class NewsApi {

    val endpoint: SimpleRest by lazy {
        SimpleRestImpl(baseUrl = "https://domain.tld/api")
    }
}

class NewsRepository(private val api: NewsApi) {
 
	// request list of items from specified page
	suspend fun fetchNews(page: Int): List&lt;NewsData&gt;? =
		api.endpoint.get("/news").addParam("page", page).awaitData()
    
	// make flow with item, updated every 15 seconds
	fun getHeadNewsLine(): NewsData =
		api.endpoint.get("/breaking").toFlow(15000)
    
    fun getFileStream(fileName: String): Response =
        api.endpoint.get("/files/download/$fileName").getRawResponse()
}
</pre>

Use RAW response and provided tools to download files:
<pre>
class FilesRepository(private val api: FilesApi) {
 
    fun getFileStream(fileName: String): Response =
        api.endpoint.get("/downloads/$fileName").getRawResponse()
}

class DownloadViewModel(private val repository: FilesRepository) {
    // Use DownloadState to observe downloading
    private val _downloadState = MutableLiveData&lt;DownloadState&gt;()
    val downloadState: LiveData&lt;DownloadState&gt; = _downloadState

    fun download(remoteFileName: String, localFileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val downloadFlow = repository.getFileStream(remoteFileName).saveFile(localFileName)
                downloadFlow.collect { _downloadState.postValue(it) }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}
</pre>

Usage with various return types:
<pre>
class DataRepository(private val api: SimpleRest) {
	
	suspend fun postData(submission: MyRequest): SimpleResponse&lt;PossibleResponses?&gt; =
		// here is full path specified
		api.post("https://domain.tld/api/submit", submission)
			// different acceptable types based on HTTP response code
			.addResponseType(200, RegularExpected::class.java)
			.addResponseType(208, PartialSuccess::class.java)
			.addResponseType(301, MovingError::class.java)
            .awaitResponse()
}

// it is possible to use Any but better way is mark all acceptable types by interface
sealed interface PossibleResponses {
    data class RegularExpected(
    	val title: String,
    ) : PossibleResponses
    
    data class PartialSuccess(
    	val summary: String,
        val amount: Double
    ) : PossibleResponses
    //etc...
}
</pre>
