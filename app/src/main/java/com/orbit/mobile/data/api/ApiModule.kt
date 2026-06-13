package com.orbit.mobile.data.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

// API DI
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationsApi(retrofit: Retrofit): NotificationsApi =
        retrofit.create(NotificationsApi::class.java)

    @Provides
    @Singleton
    fun provideProjectsApi(retrofit: Retrofit): ProjectsApi =
        retrofit.create(ProjectsApi::class.java)

    @Provides
    @Singleton
    fun provideUsersApi(retrofit: Retrofit): UsersApi =
        retrofit.create(UsersApi::class.java)

    @Provides
    @Singleton
    fun provideSystemApi(retrofit: Retrofit): SystemApi =
        retrofit.create(SystemApi::class.java)

    @Provides
    @Singleton
    fun provideAnalyticsApi(retrofit: Retrofit): AnalyticsApi =
        retrofit.create(AnalyticsApi::class.java)

    @Provides
    @Singleton
    fun provideTeamsApi(retrofit: Retrofit): TeamsApi =
        retrofit.create(TeamsApi::class.java)

    @Provides
    @Singleton
    fun provideTasksApi(retrofit: Retrofit): TasksApi =
        retrofit.create(TasksApi::class.java)

    @Provides
    @Singleton
    fun provideQcApi(retrofit: Retrofit): QcApi =
        retrofit.create(QcApi::class.java)

    @Provides
    @Singleton
    fun provideTaskBoardsApi(retrofit: Retrofit): TaskBoardsApi =
        retrofit.create(TaskBoardsApi::class.java)

    @Provides
    @Singleton
    fun provideQualityApi(retrofit: Retrofit): QualityApi =
        retrofit.create(QualityApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi =
        retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun provideEventsApi(retrofit: Retrofit): EventsApi =
        retrofit.create(EventsApi::class.java)

    @Provides
    @Singleton
    fun provideQcSuiteApi(retrofit: Retrofit): QcSuiteApi =
        retrofit.create(QcSuiteApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi =
        retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideItApi(retrofit: Retrofit): ItApi =
        retrofit.create(ItApi::class.java)

    @Provides
    @Singleton
    fun provideFounderApi(retrofit: Retrofit): FounderApi =
        retrofit.create(FounderApi::class.java)
}
