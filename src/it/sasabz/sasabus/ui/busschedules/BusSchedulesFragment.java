/*
 * SASAbus - Android app for SASA bus open data
 *
 * BusSchedulesFragment.java
 *
 * Created: Jan 27, 2014 10:55:00 AM
 *
 * Copyright (C) 2011-2014 Paolo Dongilli, Markus Windegger, Davide Montesin
 *
 * This file is part of SASAbus.
 *
 * SASAbus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SASAbus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SASAbus.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.sasabz.sasabus.ui.busschedules;

import it.sasabz.android.sasabus.R;
import it.sasabz.sasabus.opendata.client.logic.BusTripCalculator;
import it.sasabz.sasabus.opendata.client.model.BusDayType;
import it.sasabz.sasabus.opendata.client.model.BusLine;
import it.sasabz.sasabus.opendata.client.model.BusTripBusStopTime;
import it.sasabz.sasabus.opendata.client.model.BusTripStartTime;
import it.sasabz.sasabus.opendata.client.model.BusTripStartVariant;
import it.sasabz.sasabus.ui.MainActivity;
import it.sasabz.sasabus.ui.routing.DateButton;
import it.sasabz.sasabus.ui.routing.DatePicker;
import it.sasabz.sasabus.ui.routing.TimeButton;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;

public class BusSchedulesFragment extends SherlockFragment
{

   private static final String    BZ                     = "BZ";
   private static final String    ME                     = "ME";
   private static final String    OTHER                  = "OTHER";

   private Spinner                spinnerArea;
   private Spinner                spinnerBusLine;
   private ListView               listviewBuslineDepartures;

   MainActivity                   mainActivity;

   Button                         currentDate;
   Button                         currentTime;

   BusLine                        lastBusLine;

   String                         currentArea;

   static BusLineGroupException[] busLineGroupExceptions = new BusLineGroupException[]{
            new BusLineGroupException(201, new String[]{OTHER}),
            new BusLineGroupException(248, new String[]{OTHER}),
            new BusLineGroupException(300, new String[]{OTHER}),
            new BusLineGroupException(227, new String[]{ME}),
            new BusLineGroupException(5000, new String[]{OTHER}),};

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
   {
      try
      {
         this.mainActivity = (MainActivity) this.getActivity();

         final View view = inflater.inflate(R.layout.fragment_bus_schedules, container, false);

         this.currentDate = (Button) view.findViewById(R.id.currentDate);
         DateButton.init(this.currentDate);

         this.currentTime = (Button) view.findViewById(R.id.currentTime);
         TimeButton.init(this.currentTime);

         this.listviewBuslineDepartures = (ListView) view.findViewById(R.id.listview_busline_departures);

         Button recalculateDepartures = (Button) view.findViewById(R.id.recalculateDepartures);
         recalculateDepartures.setOnClickListener(new View.OnClickListener()
         {
            @Override
            public void onClick(View v)
            {
               try
               {
                  BusSchedulesFragment.this.addAdapterToListView(BusSchedulesFragment.this.lastBusLine);
               }
               catch (Exception e)
               {
                  BusSchedulesFragment.this.mainActivity.handleApplicationException(e);
                  e.printStackTrace();
               }
            }
         });

         //Spinners
         this.addEntriesToSpinnerArea(view);

         this.addEntriesToSpinnerBusline(view);

         //ListView
         this.addOnItemSelectedListenerToListView();

         return view;
      }
      catch (Exception ioxxx)
      {
         this.mainActivity.handleApplicationException(ioxxx);
         throw new RuntimeException(ioxxx);
      }
   }

   private void addEntriesToSpinnerArea(final View view)
   {
      this.spinnerArea = (Spinner) view.findViewById(R.id.spinner_area);
      ArrayList<String> areas = new ArrayList<String>();
      areas.add(this.mainActivity.getString(R.string.BusSchedulesFragment_area_all));
      areas.add(this.mainActivity.getString(R.string.BusSchedulesFragment_area_bz));
      areas.add(this.mainActivity.getString(R.string.BusSchedulesFragment_area_me));
      areas.add(this.mainActivity.getString(R.string.BusSchedulesFragment_area_other));

      ArrayAdapter<String> adapterArea = new ArrayAdapter<String>(this.getSherlockActivity(),
                                                                  android.R.layout.simple_spinner_item,
                                                                  areas);
      adapterArea.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

      this.spinnerArea.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
      {
         @Override
         public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
         {
            int index = arg2;
            switch (index)
            {
               case 0:
                  BusSchedulesFragment.this.currentArea = null;
                  break;
               case 1:
                  BusSchedulesFragment.this.currentArea = BZ;
                  break;
               case 2:
                  BusSchedulesFragment.this.currentArea = ME;
                  break;
               case 3:
                  BusSchedulesFragment.this.currentArea = OTHER;
                  break;
            }
            try
            {
               BusSchedulesFragment.this.addEntriesToSpinnerBusline(view);
            }
            catch (IOException e)
            {
               BusSchedulesFragment.this.mainActivity.handleApplicationException(e);
            }
         }

         @Override
         public void onNothingSelected(AdapterView<?> arg0)
         {

         }
      });

      this.spinnerArea.setAdapter(adapterArea);

   }

   private boolean isBusLineInArea(BusLine busLine, String area)
   {
      if (area == null)
      {
         return true;
      }
      for (BusLineGroupException exception : busLineGroupExceptions)
      {
         if (exception.LI_NR == busLine.getLI_NR())
         {
            for (String exceptionArea : exception.areas)
            {
               if (exceptionArea.equals(area))
               {
                  return true;
               }
            }
            return false;
         }
      }
      boolean isAreaInName = busLine.getShortName().endsWith(" " + area);
      return isAreaInName;
   }

   private void addEntriesToSpinnerBusline(View view) throws IOException
   {
      this.spinnerBusLine = (Spinner) view.findViewById(R.id.spinner_busline);

      ArrayList<String> busLinesNames = new ArrayList<String>();
      final ArrayList<Integer> busLineIds = new ArrayList<Integer>();
      busLinesNames.add(this.mainActivity.getString(R.string.BusSchedulesFragment_select_line));
      busLineIds.add(null);
      final BusLine[] busLines = this.mainActivity.getOpenDataStorage().getBusLines().getList();
      for (BusLine busLine : busLines)
      {
         int lineId = busLine.getLI_NR();
         if (this.isBusLineInArea(busLine, this.currentArea))
         {
            busLinesNames.add(busLine.getShortName() + " (" + lineId + ")");
            busLineIds.add(lineId);
         }
      }

      ArrayAdapter<String> adapterBusLines = new ArrayAdapter<String>(this.getSherlockActivity(),
                                                                      android.R.layout.simple_spinner_item,
                                                                      busLinesNames);
      adapterBusLines.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

      this.spinnerBusLine.setAdapter(adapterBusLines);
      this.spinnerBusLine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
      {
         @Override
         public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
         {
            int index = arg2;
            BusLine busLine = null;
            if (index > 0)
            {
               busLine = busLines[index - 1];
            }

            try
            {
               BusSchedulesFragment.this.addAdapterToListView(busLine);
            }
            catch (Exception e)
            {
               BusSchedulesFragment.this.mainActivity.handleApplicationException(e);
            }
         }

         @Override
         public void onNothingSelected(AdapterView<?> arg0)
         {

         }
      });

   }

   private void addAdapterToListView(final BusLine busLine) throws IOException, ParseException
   {
      this.lastBusLine = busLine;

      if (busLine != null)
      {
         final long startCalc = System.currentTimeMillis();

         ArrayAdapter<String> loadingAdapter = new ArrayAdapter<String>(this.getActivity(),
                                                                        android.R.layout.simple_list_item_1);
         loadingAdapter.add(this.mainActivity.getString(R.string.BusSchedulesFragment_searching));
         this.listviewBuslineDepartures.setAdapter(loadingAdapter);

         SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyy-MM-dd");

         final String day = yyyyMMdd.format(DatePicker.simpleDateFormat.parse(this.currentDate.getText().toString()));

         new Thread(new Runnable()
         {
            @Override
            public void run()
            {
               try
               {
                  ArrayList<BusDepartureItem> departures = new ArrayList<BusDepartureItem>();

                  BusDayType calendarDay = BusSchedulesFragment.this.mainActivity.getOpenDataStorage().getBusDayTypeList().findBusDayTypeByDay(day);
                  if (calendarDay == null)
                  {
                     // This day isn't in the calendar!
                     return;
                  }
                  int dayType = calendarDay.getDayTypeId();
                  String[] hh_mm = BusSchedulesFragment.this.currentTime.getText().toString().split(":");
                  int seconds = (Integer.parseInt(hh_mm[0]) * 60 + Integer.parseInt(hh_mm[1])) * 60;

                  BusTripStartVariant[] variants = BusSchedulesFragment.this.mainActivity.getOpenDataStorage().getBusTripStarts(busLine.getLI_NR(),
                                                                                                                                dayType);
                  for (BusTripStartVariant busTripStartVariant : variants)
                  {

                     BusTripStartTime[] times = busTripStartVariant.getTriplist();
                     for (BusTripStartTime busTripStartTime : times)
                     {
                        if (busTripStartTime.getSeconds() > seconds - 60 * 60 * 2 /* && busTripStartTime.getSeconds() <= seconds*/)
                        {
                           BusTripBusStopTime[] stopTimes = BusTripCalculator.calculateBusStopTimes(busLine.getLI_NR(),
                                                                                                    busTripStartVariant.getVariantId(),
                                                                                                    busTripStartTime,
                                                                                                    BusSchedulesFragment.this.mainActivity.getOpenDataStorage());

                           String destinationBusStationName = BusSchedulesFragment.this.mainActivity.getBusStationNameUsingAppLanguage(BusSchedulesFragment.this.mainActivity.getOpenDataStorage().getBusStations().findBusStop(stopTimes[stopTimes.length - 1].getBusStop()).getBusStation());

                           for (int i = 0; i < stopTimes.length - 1 /* last stop isn't show because we show only departures */; i++)
                           {
                              BusTripBusStopTime stopTime = stopTimes[i];
                              if (stopTime.getSeconds() >= seconds)
                              {
                                 String busStationName = BusSchedulesFragment.this.mainActivity.getBusStationNameUsingAppLanguage(BusSchedulesFragment.this.mainActivity.getOpenDataStorage().getBusStations().findBusStop(stopTime.getBusStop()).getBusStation());
                                 BusDepartureItem item = new BusDepartureItem(formatSeconds(stopTime.getSeconds()),
                                                                              busStationName,
                                                                              destinationBusStationName,
                                                                              stopTimes,
                                                                              i);

                                 departures.add(item);
                                 break;
                              }
                           }

                        }
                     }
                  }

                  Collections.sort(departures, new Comparator<BusDepartureItem>()
                  {
                     @Override
                     public int compare(BusDepartureItem i1, BusDepartureItem i2)
                     {
                        int diff = i1.getTime().compareTo(i2.getTime());
                        if (diff == 0)
                        {
                           i1.getBusStopOrLineName().compareTo(i2.getBusStopOrLineName());
                        }
                        if (diff == 0)
                        {
                           i1.getDestinationName().compareTo(i2.getDestinationName());
                        }
                        return diff;
                     }
                  });

                  final BusSchedulesDepartureAdapter departuresAdapter = new BusSchedulesDepartureAdapter(BusSchedulesFragment.this.getSherlockActivity(),
                                                                                                          departures);

                  BusSchedulesFragment.this.listviewBuslineDepartures.post(new Runnable()
                  {

                     @Override
                     public void run()
                     {
                        BusSchedulesFragment.this.listviewBuslineDepartures.setAdapter(departuresAdapter);

                        long stopCalc = System.currentTimeMillis();

                     }
                  });

               }
               catch (IOException ioxxx)
               {
                  BusSchedulesFragment.this.mainActivity.handleApplicationException(ioxxx);
               }
            }
         }).start();
         ;

      }

   }

   public static String formatSeconds(long seconds)
   {
      long sec = seconds % 60;
      long min = seconds / 60 % 60;
      long hour = seconds / 3600;
      return "" + twoDigits(hour) + ":" + twoDigits(min);
   }

   public static String twoDigits(long num)
   {
      String ret = "00" + num;
      ret = ret.substring(ret.length() - 2);
      return ret;
   }

   private void addOnItemSelectedListenerToListView()
   {
      this.listviewBuslineDepartures.setOnItemClickListener(new OnItemClickListener()
      {
         @Override
         public void onItemClick(AdapterView<?> parent, View view, int position, long id)
         {

            BusDepartureItem busDepartureItem = (BusDepartureItem) BusSchedulesFragment.this.listviewBuslineDepartures.getAdapter().getItem(position);

            BusScheduleDetailsFragment fragmentToShow = (BusScheduleDetailsFragment) SherlockFragment.instantiate(BusSchedulesFragment.this.getActivity(),
                                                                                                                  BusScheduleDetailsFragment.class.getName());
            fragmentToShow.setData(busDepartureItem.index,
                                   busDepartureItem.stopTimes,
                                   BusSchedulesFragment.this.lastBusLine.getShortName());
            FragmentManager fragmentManager = BusSchedulesFragment.this.getActivity().getSupportFragmentManager();
            fragmentManager.beginTransaction().add(R.id.content_frame, fragmentToShow).addToBackStack(null).commit();

         }
      });
   }

}