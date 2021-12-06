package com.example.alarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.alarmapp.databinding.ActivityMainBinding
import java.util.*

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun getLayoutResourceId() = R.layout.activity_main

    override fun initDataBinding() {

    }

    override fun initView() {
        initOnOffButton()
        initChangeAlarmTimeButton()

        val model = fetchDataFromSharedPreferences()
        renderView(model)
    }

    private fun renderView(model: AlarmDisplayModel) {
        mBinding.ampmTextView.apply {
            text = model.ampmText
        }

        mBinding.timeTextView.apply {
            text = model.timeText
        }

        mBinding.onOffButton.apply {
            text = model.onOffText
            tag = model
        }
    }

    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)

        val timeDBValue = sharedPreferences.getString(ALARM_KEY, "9:30") ?: "9:30"
        val onOffDBValue = sharedPreferences.getBoolean(ONOFF_KEY, false)
        val alarmData = timeDBValue.split(":")

        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOff = onOffDBValue
        )

        // 보정 예외처리
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            Intent(this,
                AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE) // 있으면 가져오고 없으면 안만든다null

        if((pendingIntent == null) and alarmModel.onOff) {
        // 알람은 꺼져있는데, 데이터는 켜져있는 경우.
            alarmModel.onOff = false

        } else if ((pendingIntent != null) and alarmModel.onOff.not()) {
            // 알람은 켜져있는데, 데이터는 꺼져 있는 경우
            // 알람의 취소함
            pendingIntent.cancel()
        }
        return alarmModel
    }

    private fun initOnOffButton() {
        mBinding.onOffButton.setOnClickListener {
            // 데이터를 확인한다
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener
            val newModel = saveAlarmModel(model.hour, model.minute, model.onOff.not())
            renderView(newModel)

            // 온오프에 따라 작업 처리
            if(newModel.onOff) {
                // 켜진 경우 -> 알람 등록
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)

                    // 지나간 시간의 경우 다음날 알람으로 울리도록
                    if(before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }

                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT) // 있으면 새로 만든거로 업데이트

                // 정확한 시간에 발생 시키기 위해서는
                //alarmManager.setExact()를 사용
                alarmManager.setInexactRepeating( // 리소스 부담을 줄여주기 위해 inexactAPI를 썼음
                    AlarmManager.RTC_WAKEUP,  // 실제 시간 기준으로 wakeup, ELAPSED_REALTIME_WAKEUP : 부팅 시간 기준으로 WAKEUP
                    calendar.timeInMillis,  // 언제 알림이 발동할지
                    AlarmManager.INTERVAL_DAY, // 하루에 한번
                    pendingIntent
                )
            } else {
                // 꺼진 경우 -> 알람 제거
                cancleAlarm()
            }


            // 데이터 저장
        }
    }

    private fun initChangeAlarmTimeButton() {
        mBinding.changeAlarmTimeButton.setOnClickListener {
            val calendar = Calendar.getInstance() // 캘린더를 가져와서

            TimePickerDialog(this, { picker, hour, minute ->
                val model = saveAlarmModel(hour, minute, false)   // 데이터를 저장한다.
                renderView(model)  // 뷰를 업데이트한다.

                // 기존에 있던 알람을 삭제한다.(리시버)
                cancleAlarm()

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }
    }

    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ): AlarmDisplayModel {
        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(ALARM_KEY, model.makeDataForDB())
            putBoolean(ONOFF_KEY, model.onOff)
            commit()
        }

        return model
    }

    private fun cancleAlarm() {
        // 기존에 있던 알람을 삭제한다.
        val pendingIntent = PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE)
        pendingIntent?.cancel()
    }

    companion object {
        private const val SHARED_PREFERENCE_NAME = "time"
        private const val ALARM_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
        private const val ALARM_REQUEST_CODE = 1000
    }
}