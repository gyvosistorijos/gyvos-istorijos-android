package com.trafi.istorijos;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import retrofit2.http.GET;

public class Api {

    interface StoriesService {
        @GET("hints")
        Call<List<Story>> listStories();
    }

    static StoriesService storiesService;

    public static StoriesService getStoriesService() {
        if (null == storiesService) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .client(client)
                    .baseUrl("http://istorijosmessengerbot.azurewebsites.net/api/")
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build();
            storiesService = retrofit.create(StoriesService.class);
        }
        return storiesService;
    }

}
