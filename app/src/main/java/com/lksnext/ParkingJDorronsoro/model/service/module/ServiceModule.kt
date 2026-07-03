package com.lksnext.ParkingJDorronsoro.model.service.module
import com.lksnext.ParkingJDorronsoro.model.service.AccountService
import com.lksnext.ParkingJDorronsoro.model.service.AvisoSalidaService
import com.lksnext.ParkingJDorronsoro.model.service.LogService
import com.lksnext.ParkingJDorronsoro.model.service.NotificacionService
import com.lksnext.ParkingJDorronsoro.model.service.PlazaService
import com.lksnext.ParkingJDorronsoro.model.service.ReservaService
import com.lksnext.ParkingJDorronsoro.model.service.UsuarioService
import com.lksnext.ParkingJDorronsoro.model.service.VehiculoService
import com.lksnext.ParkingJDorronsoro.model.service.impl.AccountServiceImp
import com.lksnext.ParkingJDorronsoro.model.service.impl.AvisoSalidaServiceImpl
import com.lksnext.ParkingJDorronsoro.model.service.impl.LogServiceImpl
import com.lksnext.ParkingJDorronsoro.model.service.impl.NotificacionServiceImpl
import com.lksnext.ParkingJDorronsoro.model.service.impl.PlazaServiceImpl
import com.lksnext.ParkingJDorronsoro.model.service.impl.ReservaServiceImpl
import com.lksnext.ParkingJDorronsoro.model.service.impl.UsuarioServiceImpl
import com.lksnext.ParkingJDorronsoro.model.service.impl.VehiculoServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    abstract fun provideAccountService(impl: AccountServiceImp): AccountService
    @Binds
    abstract fun provideLogService(impl: LogServiceImpl): LogService
    @Binds
    abstract fun provideUsuarioService(impl: UsuarioServiceImpl): UsuarioService
    @Binds
    abstract fun providePlazaService(impl: PlazaServiceImpl): PlazaService
    @Binds
    abstract fun provideReservaService(impl: ReservaServiceImpl): ReservaService
    @Binds
    abstract fun provideVehiculoService(impl: VehiculoServiceImpl): VehiculoService
    @Binds
    abstract fun provideNotificacionService(impl: NotificacionServiceImpl): NotificacionService
    @Binds
    abstract fun provideAvisoSalidaService(impl: AvisoSalidaServiceImpl): AvisoSalidaService
}