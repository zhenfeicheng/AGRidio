package com.project.sky31radio.data;

import com.project.sky31radio.model.Album;
import com.project.sky31radio.model.Anchor;
import com.project.sky31radio.model.Pagination;
import com.project.sky31radio.model.Program;

import java.util.Map;

import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;
import rx.Observable;

/**
 * Created by linroid on 1/14/15.
 */
public interface ApiService {
    @GET("/program")
    Observable<Pagination<Program>> listPrograms(@Query("page") int page, @QueryMap Map<String, String> params);
    @GET("/album")
    Observable<Pagination<Album>> listAlbums(@Query("page") int page);

    @GET("/anchor")
    Observable<Pagination<Anchor>> listAnchor(@Query("page") int page);

    @GET("/program/{id}")
    Observable<Program> programDetail(@Path("id") int programId);
}
