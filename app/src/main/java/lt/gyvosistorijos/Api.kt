package lt.gyvosistorijos

import lt.gyvosistorijos.entity.Story
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

object Api {

    interface StoriesService {
        @GET("hints")
        fun listStories(): Call<List<Story>>

        @POST("hints")
        fun createStory(@Body story: Story): Call<ResponseBody>
    }

    internal var storiesService: StoriesService? = null

    fun getStoriesService(): StoriesService {
        if (null == storiesService) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BASIC
            val client = OkHttpClient.Builder().addInterceptor(logging).build()

            val retrofit = Retrofit.Builder()
                    .client(client)
                    .baseUrl("http://www.gyvosistorijos.lt/api/")
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()
            storiesService = retrofit.create(StoriesService::class.java)
        }
        return storiesService!!
    }

}
