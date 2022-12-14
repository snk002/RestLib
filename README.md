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
class NewsRepository(private val api: SimpleRest) {

	// initially set endpoint address
	init {
		api.setBaseUrl("https://domain.tld/api/")
	}
  
	// request list of items from specified page
	suspend fun fetchNews(page: Int): List&lt;NewsData&gt;? =
		api.get("news").addParam("page", page).awaitData()
    
	// make flow with item, updated every 15 seconds
	fun getHeadNewsLine(): NewsData =
		api.get("breaking").toFlow(15000)
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
			.addResponseType(400, DataError::class.java)
			.addResponseType(500, ServerError::class.java).awaitResponse()
}

// it is possible to use Any but better way is mark all acceptable types by interface
interface PossibleResponses

data class RegularExpected(
	val title: String,
) : PossibleResponses

data class PartialSuccess(
	val summary: String
) : PossibleResponses

//etc...
</pre>
