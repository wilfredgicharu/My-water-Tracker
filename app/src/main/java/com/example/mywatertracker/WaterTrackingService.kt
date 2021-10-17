package com.example.mywatertracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.LocusId
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class WaterTrackingService: Service() {

    private var fluidBalanceMilliliters= 0f
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent?): IBinder?= null

    companion object{
        const val EXTRA_INTAkE_AMOUNT_MILLILiTERS= "intake"
        private const val NOTIFICATION_ID =0x3A7A
    }
    private fun getPendingIntent()= PendingIntent.getActivity(this,0,
        Intent(this,MainActivity::class.java),0
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String{
        val channelId= "FluidBalanceTracking"
        val channelName= "Fluid Balance tracking"
        val channel= NotificationChannel(channelId,channelName,NotificationManager.IMPORTANCE_DEFAULT)
        val service= getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId

    }
    private fun getNotificationBuilder(pendingIntent: PendingIntent,
    channelId: String) = NotificationCompat.Builder(this,channelId)
        .setContentTitle("Tracking your Fluid Balance")
        .setContentText("Tracking")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(pendingIntent)
        .setTicker("Fluid balance tracking started")

    private fun startForegroundService(): NotificationCompat.Builder{
        val pendingIntent= getPendingIntent()
        val channelId= if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel()
        } else{
            ""
        }
        val notificationBuilder= getNotificationBuilder(pendingIntent, channelId)
        startForeground(NOTIFICATION_ID,notificationBuilder.build())
        return notificationBuilder
    }
    private fun addToFluidBalance(amountMilliliters: Float){
        synchronized(this){
            fluidBalanceMilliliters +=amountMilliliters
        }
    }
    private fun updateFluidBalance(){
        serviceHandler.postDelayed({
            updateFluidBalance()
            addToFluidBalance(-0.144f)
            notificationBuilder.setContentText("your fluid balance: %.2f".format(fluidBalanceMilliliters)
            )
            startForeground(NOTIFICATION_ID,notificationBuilder.build())
        },5000L)
    }

    override fun onCreate() {
        super.onCreate()
        notificationBuilder= startForegroundService()
        val handlerThread= HandlerThread("Route Tracking").apply {
            start()
        }
        serviceHandler= Handler(handlerThread.looper)
        updateFluidBalance()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue= super.onStartCommand(intent, flags, startId)
        val intakeAmountMilliliters= intent?.getFloatExtra(EXTRA_INTAkE_AMOUNT_MILLILiTERS, 0f)
        intakeAmountMilliliters?.let {
            addToFluidBalance(it)
        }
        return returnValue
    }

    override fun onDestroy() {
        serviceHandler.removeCallbacksAndMessages(null)
    }

}