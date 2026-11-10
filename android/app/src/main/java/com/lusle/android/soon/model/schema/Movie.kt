package com.lusle.android.soon.model.schema

import java.io.Serializable;
import com.google.gson.annotations.Expose
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

// 모든 인자에 기본값이 있어 무인자 생성자가 생긴다 → Gson 이 누락 필드에 기본값을 남긴다.
// 명시적 null 은 NullSkippingTypeAdapterFactory 가 누락으로 바꾼다 (#11)
@JsonAdapter(NullSkippingTypeAdapterFactory::class)
data class Movie(
        @SerializedName("vote_count")
        @Expose
        val voteCount: Int = 0,

        @SerializedName("id")
        @Expose
        val id: Int = 0,

        @SerializedName("video")
        @Expose
        val video: Boolean = false,

        @SerializedName("vote_average")
        @Expose
        val voteAverage: Double = 0.0,

        @SerializedName("title")
        @Expose
        val title: String = "",

        @SerializedName("popularity")
        @Expose
        val popularity: Double = 0.0,

        @SerializedName("poster_path")
        @Expose
        val posterPath: String? = null,

        @SerializedName("original_language")
        @Expose
        val originalLanguage: String = "",

        @SerializedName("original_title")
        @Expose
        val originalTitle: String = "",

        @SerializedName("genre_ids")
        @Expose
        val genreIds: List<Int> = emptyList(),

        @SerializedName("backdrop_path")
        @Expose
        val backdropPath: String? = null,

        @SerializedName("adult")
        @Expose
        val adult: Boolean = false,

        @SerializedName("overview")
        @Expose
        val overview: String? = null,

        @SerializedName("release_date")
        @Expose
        val releaseDate: String = "",

) : Serializable {
        override fun equals(other: Any?): Boolean = (other is Movie) && id == other.id

        override fun hashCode(): Int {
                return id
        }

        override fun toString(): String {
                return "Movie{" +
                        "voteCount=" + voteCount +
                        ", id=" + id +
                        ", video=" + video +
                        ", voteAverage=" + voteAverage +
                        ", title='" + title + '\'' +
                        ", popularity=" + popularity +
                        ", posterPath='" + posterPath + '\'' +
                        ", originalLanguage='" + originalLanguage + '\'' +
                        ", originalTitle='" + originalTitle + '\'' +
                        ", genreIds=" + genreIds +
                        ", backdropPath=" + backdropPath +
                        ", adult=" + adult +
                        ", overview='" + overview + '\'' +
                        ", releaseDate='" + releaseDate + '\'' +
                        '}'
        }
}