package com.example.alarmapp

data class AlarmDisplayModel(
    val hour: Int,
    val minute: Int,
    var onOff: Boolean
) {
    val timeText: String
        get() {
            val h = "%02d".format(if (hour < 12) hour else hour - 12) // 두 자리수까지 int형 2자리 수가 넘지 않은 것은 0으로 채워주겠다.
            val m = "%02d".format(minute)

            return "$h:$m"
        }

    val ampmText: String
        get() {
            return if(hour < 12) "AM" else "PM"
        }

    val onOffText: String
        get() {
            return if (onOff) "알람 끄기" else "알람 켜기"
        }

    fun makeDataForDB(): String {
        return "$hour:$minute"
    }
}

