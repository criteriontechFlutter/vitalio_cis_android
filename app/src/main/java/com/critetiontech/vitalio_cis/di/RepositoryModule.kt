package com.critetiontech.vitalio_cis.di

import com.critetiontech.vitalio_cis.data.repository.AllergyRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.AuthRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.DoctorRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.FluidRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.MedicineRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.ProfileRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.ReportRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.SymptomRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.VitalRepositoryImpl
import com.critetiontech.vitalio_cis.domain.repository.AllergyRepository
import com.critetiontech.vitalio_cis.domain.repository.AuthRepository
import com.critetiontech.vitalio_cis.domain.repository.DoctorRepository
import com.critetiontech.vitalio_cis.domain.repository.FluidRepository
import com.critetiontech.vitalio_cis.domain.repository.MedicineRepository
import com.critetiontech.vitalio_cis.domain.repository.ProfileRepository
import com.critetiontech.vitalio_cis.domain.repository.ReportRepository
import com.critetiontech.vitalio_cis.domain.repository.SymptomRepository
import com.critetiontech.vitalio_cis.domain.repository.VitalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindVitalRepository(impl: VitalRepositoryImpl): VitalRepository
    @Binds @Singleton abstract fun bindAllergyRepository(impl: AllergyRepositoryImpl): AllergyRepository
    @Binds @Singleton abstract fun bindFluidRepository(impl: FluidRepositoryImpl): FluidRepository
    @Binds @Singleton abstract fun bindMedicineRepository(impl: MedicineRepositoryImpl): MedicineRepository
    @Binds @Singleton abstract fun bindDoctorRepository(impl: DoctorRepositoryImpl): DoctorRepository
    @Binds @Singleton abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository
    @Binds @Singleton abstract fun bindSymptomRepository(impl: SymptomRepositoryImpl): SymptomRepository
    @Binds @Singleton abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
