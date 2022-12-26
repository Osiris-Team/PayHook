/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.osiris.payhook.utils;

public class UtilsTime {
    public static final long MS_YEAR = 31556952000L;
    public static final long MS_MONTH = 2629800000L;
    public static final int MS_DAY = 86400000;
    public static final int MS_HOUR = 86400000;
    public static final int MS_MINUTE = 86400000;
    public static final int MS_SECONDS = 86400000;

    public String getFormattedString(long ms) {
        StringBuilder s = new StringBuilder();
        // years, months, days, hours, minutes, seconds
        long years, months, days, hours, minutes, seconds;


        if (ms > MS_YEAR) {
            years = ms / MS_YEAR;
            if (years >= 1) {
                s.append(years + "y");
                ms -= years * MS_YEAR;
            }
        }
        if (ms > MS_MONTH) {
            months = ms / MS_MONTH;
            if (months >= 1) {
                s.append(months + "mo");
                ms -= months * MS_MONTH;
            }
        }
        if (ms > MS_DAY) {
            days = ms / MS_DAY;
            if (days >= 1) {
                s.append(days + "d");
                ms -= days * MS_DAY;
            }
        }
        if (ms > MS_HOUR) {
            hours = ms / MS_HOUR;
            if (hours >= 1) {
                s.append(hours + "h");
                ms -= hours * MS_HOUR;
            }
        }
        if (ms > MS_MINUTE) {
            minutes = ms / MS_MINUTE;
            if (minutes >= 1) {
                s.append(minutes + "mi");
                ms -= minutes * MS_MINUTE;
            }
        }
        if (ms > MS_SECONDS) {
            seconds = ms / MS_SECONDS;
            if (seconds >= 1) {
                s.append(seconds + "s");
                ms -= seconds * MS_SECONDS;
            }
        }
        return s.toString();
    }

}
