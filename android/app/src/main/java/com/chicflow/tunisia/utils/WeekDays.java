package com.chicflow.tunisia.utils;

import java.util.Calendar;

/** Days of the week as stored in MySQL weekly_plans.day_of_week: 1 = Monday ... 7 = Sunday. */
public final class WeekDays {

    private WeekDays() { }

    public static final String[] NAMES = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    public static String name(int day) {
        return NAMES[day - 1];
    }

    /** Java's Calendar uses Sunday = 1 ... Saturday = 7. Converts to Monday = 1 ... Sunday = 7. */
    public static int fromCalendar(int calendarDayOfWeek) {
        return (calendarDayOfWeek + 5) % 7 + 1;
    }

    public static int todayIso() {
        return fromCalendar(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
    }
}
