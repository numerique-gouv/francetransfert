/*
  * Copyright (c) Direction Interministérielle du Numérique 
  * 
  * SPDX-License-Identifier: Apache-2.0 
  * License-Filename: LICENSE.txt 
  */

package fr.gouv.culture.francetransfert;

import fr.gouv.culture.francetransfert.domain.utils.DateUtils;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class DateUtilsTest {


    @Test
    public void testIsSameInstant_Date() {
        Date datea = new GregorianCalendar(2004, 6, 9, 13, 45).getTime();
        Date dateb = new GregorianCalendar(2004, 6, 9, 13, 45).getTime();
        TestCase.assertTrue(DateUtils.isSameInstant(datea, dateb));
        dateb = new GregorianCalendar(2004, 6, 10, 13, 45).getTime();
        assertFalse(DateUtils.isSameInstant(datea, dateb));
        datea = new GregorianCalendar(2004, 6, 10, 13, 45).getTime();
        assertTrue(DateUtils.isSameInstant(datea, dateb));
        dateb = new GregorianCalendar(2005, 6, 10, 13, 45).getTime();
        assertFalse(DateUtils.isSameInstant(datea, dateb));
    }


    @Test
    public void testIsSameDay_Date() {
        Date datea = new GregorianCalendar(2004, 6, 9, 13, 45).getTime();
        Date dateb = new GregorianCalendar(2004, 6, 9, 13, 45).getTime();
        assertTrue(DateUtils.isSameDay(datea, dateb));
        dateb = new GregorianCalendar(2004, 6, 10, 13, 45).getTime();
        assertFalse(DateUtils.isSameDay(datea, dateb));
        datea = new GregorianCalendar(2004, 6, 10, 13, 45).getTime();
        assertTrue(DateUtils.isSameDay(datea, dateb));
        dateb = new GregorianCalendar(2005, 6, 10, 13, 45).getTime();
        assertFalse(DateUtils.isSameDay(datea, dateb));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testIsSameDay_DateNotNullNull() {
        DateUtils.isSameDay(new Date(), null);
    }

    @Test
    public void testIsSameDay_Cal() {
        final GregorianCalendar cala = new GregorianCalendar(2019, 6, 9, 13, 45);
        final GregorianCalendar calb = new GregorianCalendar(2019, 6, 9, 13, 45);
        assertTrue(DateUtils.isSameDay(cala, calb));
        calb.add(Calendar.DAY_OF_YEAR, 1);
        assertFalse(DateUtils.isSameDay(cala, calb));
        cala.add(Calendar.DAY_OF_YEAR, 1);
        assertTrue(DateUtils.isSameDay(cala, calb));
        calb.add(Calendar.YEAR, 1);
        assertFalse(DateUtils.isSameDay(cala, calb));
    }


    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionForInstants() {
        DateUtils.isSameInstant(null,new Date());
    }


    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionForCalendars() {
        final GregorianCalendar cala = new GregorianCalendar(2019, 6, 9, 13, 45);

        DateUtils.isSameDay(null,cala);
    }


}
