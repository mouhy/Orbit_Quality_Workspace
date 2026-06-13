package com.orbit.mobile.data.repository

import com.orbit.mobile.domain.repository.AuthRepository
import com.orbit.mobile.domain.repository.BoardRepository
import com.orbit.mobile.domain.repository.DashboardRepository
import com.orbit.mobile.domain.repository.MemberAnalyticsRepository
import com.orbit.mobile.domain.repository.NotificationsRepository
import com.orbit.mobile.domain.repository.ProjectsRepository
import com.orbit.mobile.domain.repository.QualityRepository
import com.orbit.mobile.domain.repository.TasksRepository
import com.orbit.mobile.domain.repository.TeamsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Repo DI
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindNotificationsRepository(
        impl: NotificationsRepositoryImpl
    ): NotificationsRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindProjectsRepository(impl: ProjectsRepositoryImpl): ProjectsRepository

    @Binds
    @Singleton
    abstract fun bindTeamsRepository(impl: TeamsRepositoryImpl): TeamsRepository

    @Binds
    @Singleton
    abstract fun bindTasksRepository(impl: TasksRepositoryImpl): TasksRepository

    @Binds
    @Singleton
    abstract fun bindMemberAnalyticsRepository(
        impl: MemberAnalyticsRepositoryImpl
    ): MemberAnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindBoardRepository(impl: BoardRepositoryImpl): BoardRepository

    @Binds
    @Singleton
    abstract fun bindQualityRepository(impl: QualityRepositoryImpl): QualityRepository
}
