package org.ieszaidinvergeles.dam.chaterbot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import org.ieszaidinvergeles.dam.chaterbot.api.ChatterBot;
import org.ieszaidinvergeles.dam.chaterbot.api.ChatterBotFactory;
import org.ieszaidinvergeles.dam.chaterbot.api.ChatterBotSession;
import org.ieszaidinvergeles.dam.chaterbot.api.ChatterBotType;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

//https://github.com/pierredavidbelanger/chatter-bot-api

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int CTE = 1;
    private TextToSpeech mTts;
    private static final String TAG = "TAGGG";
    private Button btSend;
    private ImageButton btRecord;
    private EditText etTexto;
    private ScrollView svScroll;
    private TextView tvTexto;

    private ChatterBot bot;
    private ChatterBotFactory factory;
    private ChatterBotSession botSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        btSend = findViewById(R.id.btSend);
        btRecord = findViewById(R.id.imageButton);
        etTexto = findViewById(R.id.etTexto);
        svScroll = findViewById(R.id.svScroll);
        tvTexto = findViewById(R.id.tvTexto);
        if(startBot()) {
            setEvents();
        }

    }

    private String chat(final String text) {
        String response;
        try {
            response = getString(R.string.bot) + " " + botSession.think(text);
        } catch (final Exception e) {
            response = getString(R.string.exception) + " " + e.toString();
        }
        return response;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CTE && resultCode == RESULT_OK){
            ArrayList<String> textos = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (textos.size()>0){
                etTexto.setText("");
                etTexto.setText(textos.get(0));
                final String text = getString(R.string.you) + " " + etTexto.getText().toString().trim();
                btSend.setEnabled(false);
                tvTexto.append(text + "\n");
                System.out.println("holaaaaa" + textos.get(0));
                new conversacionBot().execute();
            }
        }
    }

    private void setEvents() {
        btSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String text = getString(R.string.you) + " " + etTexto.getText().toString().trim();
                btSend.setEnabled(false);
                etTexto.setText("");
                tvTexto.append(text + "\n");
                new conversacionBot().execute();
            }
        });
        btRecord.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "es-ES");
                i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora");
                i.putExtra(RecognizerIntent. EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
                startActivityForResult(i, CTE);
            }
        });
        mTts = new TextToSpeech(this,
                this  // TextToSpeech.OnInitListener
        );




    }

    private String traducir(final String text,final String from,final String to){

        AsyncTask task = new AsyncTask() {
            String response = "";

            @Override
            protected Object doInBackground(Object[] objects) {

                String trad = text;
                String link = "&text=";
                try {
                    URL url = new URL("https://www.bing.com/ttranslate?&category=&IG=51C950C044BE4176885CCAFA7B90FD83&IID=translator.5034.22");
                    URLConnection conexion = url.openConnection();
                    conexion.setDoOutput(true);
                    OutputStreamWriter out = new OutputStreamWriter(
                            conexion.getOutputStream());
                    link += URLEncoder.encode(trad, "UTF-8");
                    link = link.replace("+", "%20");
                    link +="&from="+from+ "&to="+to;
                    //out.write("&text=i%20am%20the%20one%20that%20do%20this%20request.%20&from=en&to=es");
                    out.write(link);
                    out.close();
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            conexion.getInputStream()));
                    String linea;
                    while ((linea = in.readLine()) != null) {
                        response += linea;
                    }
                    in.close();
                    JSONObject reader = null;

                    reader = new JSONObject(response);
                    response = reader.getString("translationResponse");
                    System.out.println("esta es la respuesta en hilo " + response);
                    return response;

                }catch (Exception e){
                    System.out.println(e.toString());
                }
                return response;
            }
        };
        try {
            return task.execute().get().toString();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }



    private boolean startBot() {
        boolean result = true;
        String initialMessage;
        factory = new ChatterBotFactory();
        try {
            bot = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
            botSession = bot.createSession();
            initialMessage = getString(R.string.messageConnected) + "\n";
        } catch(Exception e) {
            initialMessage = getString(R.string.messageException) + "\n" + getString(R.string.exception) + " " + e.toString();
            result = false;
        }
        tvTexto.setText(initialMessage);
        return result;
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void sayResponse(String s) {
        mTts.speak(s, TextToSpeech.QUEUE_FLUSH,  // Drop all pending entries in the playback queue.
                null);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = mTts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language is not available.");
            } else {
                sayResponse("hola");
            }
        } else {
            Log.e(TAG, "Could not initialize TextToSpeech.");
        }
    }

    class conversacionBot extends AsyncTask{
        String response;

        @Override
        protected String doInBackground(Object[] objects) {
            String text = getString(R.string.you) + " " + etTexto.getText().toString().trim();
            response = chat(traducir(text, "es", "en"));
            return text;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            etTexto.requestFocus();
            tvTexto.append(traducir(response, "en", "es") + "\n");
            sayResponse(response);
            svScroll.fullScroll(View.FOCUS_DOWN);
            btSend.setEnabled(true);
            hideKeyboard();
        }
    }
    
}