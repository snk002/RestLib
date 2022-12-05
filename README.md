# RestLib
REST library based on OkHttp3

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
