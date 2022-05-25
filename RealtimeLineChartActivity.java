
package com.xxmassdeveloper.mpchartexample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.ContextCompat;

import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.xxmassdeveloper.mpchartexample.notimportant.DemoBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.types.AblyException;
import io.ably.lib.types.ChannelOptions;
import io.ably.lib.types.Message;
import io.ably.lib.types.PaginatedResult;
import io.ably.lib.types.Param;

public class RealtimeLineChartActivity extends DemoBase implements
        OnChartValueSelectedListener {

    private LineChart chart;

    private final int[] colors = ColorTemplate.VORDIPLOM_COLORS;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_realtime_linechart);

        setTitle("Hamilton Spanish Grand Prix");

        TextView currentLap = findViewById(R.id.currentLap);
        TextView previousLap = findViewById(R.id.previousLap);
        chart = findViewById(R.id.chart1);
        chart.setOnChartValueSelectedListener(this);

        // enable description text
        chart.getDescription().setEnabled(true);

        // enable touch gestures
        chart.setTouchEnabled(true);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(true);

        // set an alternative background color
        chart.setBackgroundColor(Color.LTGRAY);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        for (int z = 0; z < 1; z++) {

            ArrayList<Entry> values = new ArrayList<>();

            LineDataSet d = new LineDataSet(values, "DataSet " + (z + 1));
            d.setLineWidth(2.5f);
            d.setCircleRadius(4f);

            //int color = colors[z % colors.length];
            //d.setColor(color);
            //d.setCircleColor(color);
            dataSets.add(d);
        }

        // make the first DataSet dashed
        ((LineDataSet) dataSets.get(0)).enableDashedLine(10, 10, 0);
        //((LineDataSet) dataSets.get(1)).enableDashedLine(10, 10, 0);
        //((LineDataSet) dataSets.get(0)).setColors(ColorTemplate.VORDIPLOM_COLORS);
        //((LineDataSet) dataSets.get(1)).setColors(ColorTemplate.VORDIPLOM_COLORS);
        //((LineDataSet) dataSets.get(0)).setCircleColors(ColorTemplate.VORDIPLOM_COLORS);

        LineData data = new LineData(dataSets);

        data.setValueTextColor(Color.WHITE);
        // add empty data

        chart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();

        // modify the legend ...
        l.setForm(LegendForm.LINE);
        l.setTypeface(tfLight);
        l.setTextColor(Color.WHITE);

        XAxis xl = chart.getXAxis();
        xl.setTypeface(tfLight);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTypeface(tfLight);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(400f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
        feedMultiple();
    }

    private void addEntry(int myValue) {

        LineData data = chart.getData();
        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), myValue), 0);

            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            // limit the number of visible entries
            chart.setVisibleXRangeMaximum(120);
            // chart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // chart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    private Thread thread;

    private void feedMultiple() {
        if (thread != null)
            thread.interrupt();
        TextView currentLap = findViewById(R.id.currentLap);
        TextView previousLap = findViewById(R.id.previousLap);

        try {
            setCurLapHistory();
            setPrevLapHistory();
        } catch (Exception e) {
        }

        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                //addEntry();
            }
        };

        final Map<String, String> params = new HashMap<>();
        params.put("rewind", "1");
        final ChannelOptions options = new ChannelOptions();
        options.params = params;

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    AblyRealtime ably = new AblyRealtime("973y9Q.GV9S4g:x-SDde68V2RiCvQqZP14sTo_S6U89L6etFtz769S04U");
                    Channel speedChannel = ably.channels.get("speed");
                    Channel lapChannel = ably.channels.get("lap");

                    speedChannel.subscribe(new Channel.MessageListener() {
                        @Override
                        public void onMessage(Message message) {
                            Log.d("Ably","Received `" + message.name + "` message with data: " + message.data);
                            addEntry(Integer.parseInt(message.data.toString()));
                            System.out.println("Received `" + message.name + "` message with data: " + message.data.toString());
                        }
                    });

                    lapChannel.subscribe(new Channel.MessageListener() {
                        @Override
                        public void onMessage(Message message) {
                            Log.d("Ably","Received `" + message.name + "` message with data: " + message.data);
                            currentLap.setText(message.data.toString());
                            try {
                                setPrevLapHistory();
                            } catch (AblyException e) {
                                e.printStackTrace();
                            }
                            System.out.println("Received `" + message.name + "` message with data: " + message.data.toString());

                        }
                    });
                } catch (Exception e){
                    e.printStackTrace();
                }
                for (int i = 0; i < 1000; i++) {

                    // Don't generate garbage runnables inside the loop.
                    runOnUiThread(runnable);

                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.realtime, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.viewGithub: {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://github.com/PhilJay/MPAndroidChart/blob/master/MPChartExample/src/com/xxmassdeveloper/mpchartexample/RealtimeLineChartActivity.java"));
                startActivity(i);
                break;
            }
            case R.id.actionAdd: {
                addEntry(123);
                break;
            }
            case R.id.actionClear: {
                chart.clearValues();
                Toast.makeText(this, "Chart cleared!", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.actionFeedMultiple: {
                feedMultiple();
                break;
            }
            case R.id.actionSave: {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    saveToGallery();
                } else {
                    requestStoragePermission(chart);
                }
                break;
            }
        }
        return true;
    }

    @Override
    protected void saveToGallery() {
        saveToGallery(chart, "RealtimeLineChartActivity");
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("Entry selected", e.toString());
    }

    @Override
    public void onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (thread != null) {
            thread.interrupt();
        }
    }

    private void setPrevLapHistory() throws AblyException {
        try {
            AblyRealtime ably = new AblyRealtime("973y9Q.GV9S4g:x-SDde68V2RiCvQqZP14sTo_S6U89L6etFtz769S04U");
            Channel lapChannel = ably.channels.get("lap");
            TextView previousLap = findViewById(R.id.previousLap);
            PaginatedResult<Message> result = lapChannel.history(null);
            Message lastMessage = result.items()[1];
            System.out.println("Last message: " + lastMessage.id + " - " + lastMessage.data.toString());
            previousLap.setText(lastMessage.data.toString());
        } catch (AblyException e) {}
    }

    private void setCurLapHistory() throws AblyException {
        try {
            AblyRealtime ably = new AblyRealtime("973y9Q.GV9S4g:x-SDde68V2RiCvQqZP14sTo_S6U89L6etFtz769S04U");
            Channel lapChannel = ably.channels.get("lap");
            TextView currentLap = findViewById(R.id.currentLap);
            PaginatedResult<Message> result = lapChannel.history(null);
            Message lastMessage = result.items()[0];
            System.out.println("Last message: " + lastMessage.id + " - " + lastMessage.data.toString());
            currentLap.setText(lastMessage.data.toString());
        } catch (AblyException e){}
    }
}
