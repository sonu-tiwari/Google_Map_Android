package in.co.chicmic.mapsapi.activities;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import in.co.chicmic.mapsapi.R;
import in.co.chicmic.mapsapi.adapters.PlaceArrayAdapter;
import in.co.chicmic.mapsapi.utilities.AppConstants;
import in.co.chicmic.mapsapi.utilities.DirectionsJSONParser;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
        , GoogleMap.OnMapClickListener
        , View.OnClickListener
        , GoogleApiClient.OnConnectionFailedListener
        , GoogleApiClient.ConnectionCallbacks
{
    private GoogleMap mMap;
    private final List<LatLng> listOfPlaces = new ArrayList<>();
    private int numOfPlacesEntered;
    private Button mShowDirectionButton;
    private Button mClearButton;
    private AutoCompleteTextView mSearchACTV;
    private Button mClearTextButton;
    private static final LatLngBounds BOUNDS_INDIA =
            new LatLngBounds(new LatLng(23.63936, 68.14712), new LatLng(28.20453, 97.34466));

    private static final String LOG_TAG = "MainActivity";
    private static final int GOOGLE_API_CLIENT_ID = 0;

    private GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mPlaceArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        initViews();
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initViews() {
        mShowDirectionButton = findViewById(R.id.btn_get_direction);
        mClearButton = findViewById(R.id.btn_clear);
        mClearTextButton = findViewById(R.id.clear);
        mSearchACTV = findViewById(R.id.actv_choose_places);
        mSearchACTV.setThreshold(3);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setListeners();
        LatLng chandigarh = new LatLng(AppConstants.sLAT_CHANDIGARH, AppConstants.sLNG_CHANDIGARH);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(chandigarh));
        mMap.setMinZoomPreference(AppConstants.sLINE_WIDTH);
        mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1,
                BOUNDS_INDIA, null);
        mSearchACTV.setAdapter(mPlaceArrayAdapter);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if(charSequence.length() != 0) {
                mClearTextButton.setVisibility(View.VISIBLE);
            } else {
                mClearTextButton.setVisibility(View.GONE);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private final AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            Log.i(LOG_TAG, getString(R.string.selected) + item.description);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            Log.i(LOG_TAG, getString(R.string.fetching) + item.placeId);
            String url = AppConstants.sPLACE_BASE_URL
                    + placeId
                    + AppConstants.sKEY_AND
                    + AppConstants.sKEY
                    + AppConstants.sSENSOR_TRUE;
            DownloadPlaceData data = new DownloadPlaceData();
            data.execute(url);
        }
    };

    private final ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e(LOG_TAG, getString(R.string.places_query_error) +
                        places.getStatus().toString());
            }
        }
    };


    private void setListeners() {
        mMap.setOnMapClickListener(this);
        mShowDirectionButton.setOnClickListener(this);
        mClearButton.setOnClickListener(this);
        mSearchACTV.setOnItemClickListener(mAutocompleteClickListener);
        mClearTextButton.setOnClickListener(this);
        mSearchACTV.addTextChangedListener(mTextWatcher);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (numOfPlacesEntered < AppConstants.sNUM_OF_PLACES_TO_DRAW) {
            mMap.addMarker(new MarkerOptions().position(latLng)
                    .title(getString(R.string.position) + (numOfPlacesEntered + 1)));
            CameraPosition camPos = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(18)
                    .bearing(AppConstants.sNUM_ZERO)
                    .tilt(70)
                    .build();
            CameraUpdate camUpd3 = CameraUpdateFactory.newCameraPosition(camPos);
            mMap.animateCamera(camUpd3);
            listOfPlaces.add(latLng);
            numOfPlacesEntered++;
        } else {
            Toast.makeText(this, R.string.you_already_entered_2_places
                    , Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_get_direction:
                showPath();
                break;
            case R.id.btn_clear:
                mClearButton.setVisibility(View.GONE);
                clearViews();
                break;
            case R.id.clear:
                mSearchACTV.setText(AppConstants.sEMPTY_STRING);
        }
    }

    private void clearViews() {
        mMap.clear();
        listOfPlaces.clear();
        numOfPlacesEntered = 0;
        mSearchACTV.setText(null);
    }

    private void showPath() {
        if (numOfPlacesEntered < AppConstants.sNUM_OF_PLACES_TO_DRAW){
            Toast.makeText(this, R.string.please_select_2_positions
                    , Toast.LENGTH_SHORT).show();
        } else {
            mClearButton.setVisibility(View.VISIBLE);
            showRealDirection();
        }
    }

    private void showRealDirection() {
        LatLng origin = listOfPlaces.get(AppConstants.sNUM_ZERO);
        LatLng dest = listOfPlaces.get(AppConstants.sNUM_ONE);
        String url = getDirectionsUrl(origin, dest);
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(url);
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // Origin of route
        String str_origin = AppConstants.sORIGIN
                + origin.latitude
                + AppConstants.sCOMMA
                + origin.longitude;

        // Destination of route
        String str_dest = AppConstants.sDESTINATION
                + dest.latitude
                + AppConstants.sCOMMA
                + dest.longitude;

        // Building the parameters to the web service
        String parameters = str_origin
                + AppConstants.sAND_SIGN
                + str_dest
                + AppConstants.sAND_SIGN
                + AppConstants.sSENSOR_FALSE;

        return AppConstants.sBASE_URL_FOR_DIRECTION
                + AppConstants.sOUTPUT_FORMAT
                + AppConstants.sQUESTION_MARK
                + parameters
                + AppConstants.sKEY_AND
                + AppConstants.sKEY;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = AppConstants.sEMPTY_STRING;
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuilder sb  = new StringBuilder();

            String line;
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d(getString(R.string.exception_downloading_url), e.toString());
        }finally{
            assert iStream != null;
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private LatLng getLatLngFromURL(String pUrl) {
        StringBuilder jsonResults = new StringBuilder();
        HttpURLConnection conn = null;
        try {
            URL url = new URL(pUrl);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, getString(R.string.error_processing), e);
            return null;
        } catch (IOException e) {
            Log.e(LOG_TAG, getString(R.string.error_connecting), e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            Log.i(LOG_TAG, "getLatLngFromURL: " + jsonObj.toString());
            String status = jsonObj.getString(AppConstants.sSTATUS);
            Log.i(LOG_TAG, "getLatLngFromURL: " + status );
            if (status.equalsIgnoreCase(AppConstants.sOK)){
                jsonObj = jsonObj.getJSONObject(AppConstants.sRESULT)
                        .getJSONObject(AppConstants.sGEOMETRY)
                        .getJSONObject(AppConstants.sLOCATION);
                double lat = Double.parseDouble(jsonObj.getString(AppConstants.sLAT));
                double lng = Double.parseDouble(jsonObj.getString(AppConstants.sLNG));
                Log.i(LOG_TAG, "getLatLngFromURL: " + lat + " " + lng );
                return new LatLng(lat, lng);
            } else {
                Log.e(LOG_TAG, status + getString(R.string.status_error));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, getString(R.string.can_not_process_json), e);
        }
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void showToast(String status) {
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = AppConstants.sEMPTY_STRING;

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d(getString(R.string.background_task),e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }


    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for(int pathItems=0; pathItems<result.size(); pathItems++){
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching pathItems-th route
                List<HashMap<String, String>> path = result.get(pathItems);

                // Fetching all the points in pathItems-th route
                for(int pointsOnPath=0; pointsOnPath<path.size(); pointsOnPath++){
                    HashMap<String,String> point = path.get(pointsOnPath);

                    double lat = Double.parseDouble(point.get(AppConstants.sLAT));
                    double lng = Double.parseDouble(point.get(AppConstants.sLNG));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(AppConstants.sLINE_WIDTH);
                lineOptions.color(Color.RED);
            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                mMap.addPolyline(lineOptions);
            } else {
                showToast(AppConstants.sQUERY_LIMIT_ERROR);
            }
        }
    }


    private class DownloadPlaceData extends AsyncTask<String, Void, LatLng>{

        @Override
        protected LatLng doInBackground(String... strings) {
            return getLatLngFromURL(strings[0]);
        }

        @Override
        protected void onPostExecute(LatLng latLng) {
            super.onPostExecute(latLng);
            if (latLng != null){
                onMapClick(latLng);
            }
        }
    }

}
