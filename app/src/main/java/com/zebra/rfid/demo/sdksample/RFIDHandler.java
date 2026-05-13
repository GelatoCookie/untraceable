package com.zebra.rfid.demo.sdksample;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.AccessFilter;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.FILTER_MATCH_PATTERN;
import com.zebra.rfid.api3.GEN2V2_OPERATION_CODE;
import com.zebra.rfid.api3.Gen2v2;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagAccess;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;
import com.zebra.rfid.api3.UNTRACEABLE_RANGE;
import com.zebra.rfid.api3.UNTRACEABLE_TID;
import com.zebra.rfid.api3.accessOp.AccessOperations;

import java.util.ArrayList;

/**
 * Manages RFID reader operations including inventory, untraceable command, and tag access.
 * Handles connectivity, configuration, and event notifications from the RFID reader.
 */
class RFIDHandler implements Readers.RFIDReaderEventHandler {

    final static String TAG = "RFID_SAMPLE";
    // RFID Reader
    private static Readers readers;
    private static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private EventHandler eventHandler;
    // UI and context
    TextView textView;
    private MainActivity context;
    // general
    private int MAX_POWER = 270;
    // In case of RFD8500 change reader name with intended device below from list of paired RFD8500
    String readername = "RFD8500123";

    void onCreate(MainActivity activity) {
        // application context
        context = activity;
        // Status UI
        textView = activity.statusTextViewRFID;
        // SDK
        InitSDK();
    }

    // TEST BUTTON functionality
    // following two tests are to try out different configurations features

    /**
     * Test antenna configuration: reduces transmit power to 100.
     * @return Status message
     */
    public String Test1() {
        // check reader connection
        if (!isReaderConnected())
            return "Not connected";
        // set antenna configurations - reducing power to 100
        try {
            Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(100);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
            return e.getResults().toString() + " " + e.getVendorMessage();
        }
        return "Antenna power set to 220";
    }

    /**
     * Test singulation configuration: sets session to S2.
     * @return Status message
     */
    public String Test2() {
        // check reader connection
        if (!isReaderConnected())
            return "Not connected";
        // Set the singulation control to S2 which will read each tag once only
        try {
            Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
            s1_singulationControl.setSession(SESSION.SESSION_S2);
            s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
            s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
            return e.getResults().toString() + " " + e.getVendorMessage();
        }
        return "Session set to S2";
    }

    /**
     * Applies default reader settings: max power (270) and session S0.
     * @return Status message
     */
    public String Defaults() {
        // check reader connection
        if (!isReaderConnected())
            return "Not connected";
        try {
            // Power to 270
            Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(MAX_POWER);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);
            // singulation to S0
            Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
            s1_singulationControl.setSession(SESSION.SESSION_S0);
            s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
            s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
            reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
            return e.getResults().toString() + " " + e.getVendorMessage();
        }
        return "Default settings applied";
    }

    private boolean isReaderConnected() {
        if (reader != null && reader.isConnected())
            return true;
        else {
            Log.d(TAG, "reader is not connected");
            return false;
        }
    }

    //
    //  Activity life cycle behavior
    //

    String onResume() {
        return connect();
    }

    void onPause() {
        disconnect();
    }

    void onDestroy() {
        dispose();
    }

    //
    // RFID SDK
    //

    private void InitSDK() {
        Log.d(TAG, "InitSDK");
        if (readers == null) {
            new CreateInstanceTask().execute();
        } else
            new ConnectionTask().execute();
    }

    // Enumerates SDK based on host device
    private class CreateInstanceTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "CreateInstanceTask");
            // Based on support available on host device choose the reader type
            try {
                readers = new Readers(context, ENUM_TRANSPORT.ALL);
                availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            new ConnectionTask().execute();
        }
    }

    private class ConnectionTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            Log.d(TAG, "ConnectionTask");
            GetAvailableReader();
            if (reader != null)
                return connect();
            return "Failed to find or connect reader";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            textView.setText(result);
        }
    }

    private synchronized void GetAvailableReader() {
        Log.d(TAG, "GetAvailableReader");
        if (readers != null) {
            readers.attach(this);
            try {
                if (readers.GetAvailableRFIDReaderList() != null) {
                    availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                    if (availableRFIDReaderList.size() != 0) {
                        // if single reader is available then connect it
                        if (availableRFIDReaderList.size() == 1) {
                            readerDevice = availableRFIDReaderList.get(0);
                            reader = readerDevice.getRFIDReader();
                        } else {
                            // search reader specified by name
                            for (ReaderDevice device : availableRFIDReaderList) {
                                if (device.getName().equals(readername)) {
                                    readerDevice = device;
                                    reader = readerDevice.getRFIDReader();
                                }
                            }
                        }
                    }
                }
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            }
        }
    }

    // handler for receiving reader appearance events
    @Override
    public void RFIDReaderAppeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderAppeared " + readerDevice.getName());
        new ConnectionTask().execute();
    }

    @Override
    public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderDisappeared " + readerDevice.getName());
        if (readerDevice.getName().equals(reader.getHostName()))
            disconnect();
    }

    private synchronized String connect() {
        if (reader != null) {
            Log.d(TAG, "connect " + reader.getHostName());
            try {
                if (!reader.isConnected()) {
                    // Establish connection to the RFID Reader
                    reader.connect();
                    ConfigureReader();
                    return "Connected";
                }
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
                Log.d(TAG, "OperationFailureException " + e.getVendorMessage());
                String des = e.getResults().toString();
                return "Connection failed" + e.getVendorMessage() + " " + des;
            }
        }
        return "";
    }

    private void ConfigureReader() {
        Log.d(TAG, "ConfigureReader " + reader.getHostName());
        if (reader.isConnected()) {
            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
            try {
                // receive events from reader
                if (eventHandler == null)
                    eventHandler = new EventHandler();
                reader.Events.addEventsListener(eventHandler);
                // HH event
                reader.Events.setHandheldEvent(true);
                // tag event with tag data
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);
                // set trigger mode as rfid so scanner beam will not come
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                // set start and stop triggers
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);
                // power levels are index based so maximum power supported get the last one
                MAX_POWER = reader.ReaderCapabilities.getTransmitPowerLevelValues().length - 1;
                // set antenna configurations
                Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
                config.setTransmitPowerIndex(MAX_POWER);
                config.setrfModeTableIndex(0);
                config.setTari(0);
                reader.Config.Antennas.setAntennaRfConfig(1, config);
                // Set the singulation control
                Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
                s1_singulationControl.setSession(SESSION.SESSION_S0);
                s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
                s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
                reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
                // delete any prefilters
                reader.Actions.PreFilters.deleteAll();
                //
            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void disconnect() {
        Log.d(TAG, "disconnect " + reader);
        try {
            if (reader != null) {
                reader.Events.removeEventsListener(eventHandler);
                reader.disconnect();
                context.runOnUiThread(() -> textView.setText("Disconnected"));
            }
        } catch (InvalidUsageException | OperationFailureException e) {
            e.printStackTrace();
        }
    }

    private synchronized void dispose() {
        try {
            if (readers != null) {
                reader = null;
                readers.Dispose();
                readers = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts inventory operation on the RFID reader.
     */
    synchronized void performInventory() {
        // check reader connection
        if (!isReaderConnected())
            return;
        try {
            reader.Actions.purgeTags();
            reader.Actions.Inventory.perform();
        } catch (InvalidUsageException | OperationFailureException e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes the Untraceable command to hide EPC (reduce to 2 words) and all TID for matching tags.
     * Targets Ucode 9xe tags with EPC pattern 0x33333333.
     */
    // Untraceable: Operation lets user decide which memory bank to show and what length.
    // UntraceableParams contain the settings and password.
    // AccessFilter contains the tag pattern on which the operation occurs
    synchronized void performUntraceable() {
        // check reader connection
        if (!isReaderConnected())
            return;
        try {
            reader.Actions.purgeTags();

            AccessFilter accessFilter = new AccessFilter();
            byte[] tagMask = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
            accessFilter.TagPatternA.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
            accessFilter.TagPatternA.setTagPattern("33333333");
            accessFilter.TagPatternA.setTagPatternBitCount(32);
            accessFilter.TagPatternA.setBitOffset(32); // offset=2 words (32 bits)
            accessFilter.TagPatternA.setTagMask(tagMask);
            accessFilter.TagPatternA.setTagMaskBitCount(tagMask.length * 8);
            accessFilter.setAccessFilterMatchPattern(FILTER_MATCH_PATTERN.A);

            Gen2v2 gen2V2 = new Gen2v2();
            Gen2v2.UntraceableParams untraceableParams = gen2V2.new UntraceableParams();
            untraceableParams.setPassword(Long.parseLong("00000001", 16));
            untraceableParams.setShowEpc(true);
            untraceableParams.setHideEpc(false);
            untraceableParams.setEpcLen(2);
            untraceableParams.setTid(UNTRACEABLE_TID.HIDE_ALL_TID);
            reader.Actions.gen2v2Access.untraceable(untraceableParams, accessFilter, null);
        } catch (InvalidUsageException | OperationFailureException e) {
            e.printStackTrace();
        }
    }

    /**
     * Restores full EPC (6 words) and TID visibility for tags hidden by performUntraceable().
     */
    synchronized void restorePublicAccess() {
        // check reader connection
        if (!isReaderConnected())
            return;
        try {
            reader.Actions.purgeTags();

            AccessFilter accessFilter = new AccessFilter();
            byte[] tagMask = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
            accessFilter.TagPatternA.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
            accessFilter.TagPatternA.setTagPattern("33333333");
            accessFilter.TagPatternA.setTagPatternBitCount(16 * 2);
            accessFilter.TagPatternA.setBitOffset(32); // offset=2 words (32 bits)
            accessFilter.TagPatternA.setTagMask(tagMask);
            accessFilter.TagPatternA.setTagMaskBitCount(tagMask.length * 8);
            accessFilter.setAccessFilterMatchPattern(FILTER_MATCH_PATTERN.A);

            Gen2v2 gen2V2 = new Gen2v2();
            Gen2v2.UntraceableParams untraceableParams = gen2V2.new UntraceableParams();
            untraceableParams.setPassword(Long.parseLong("00000001", 16));
            untraceableParams.setShowEpc(true);
            untraceableParams.setHideEpc(false);
            untraceableParams.setEpcLen(6);
            untraceableParams.setTid(UNTRACEABLE_TID.SHOW_TID);
            untraceableParams.setShowUser(true);

            reader.Actions.gen2v2Access.untraceable(untraceableParams, accessFilter, null);
        } catch (InvalidUsageException | OperationFailureException e) {
            e.printStackTrace();
        }
    }

    synchronized void performAccessTID() {
        // check reader connection
        if (!isReaderConnected())
            return;
        try {
            TagAccess tagAccess = new TagAccess();
            
            // Read TID
            TagAccess.ReadAccessParams readTID = tagAccess.new ReadAccessParams();
            readTID.setMemoryBank(MEMORY_BANK.MEMORY_BANK_TID);
            readTID.setOffset(0);
            readTID.setCount(0);
            reader.Actions.TagAccess.readEvent(readTID, null, null);

            // Read EPC
            TagAccess.ReadAccessParams readEPC = tagAccess.new ReadAccessParams();
            readEPC.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
            readEPC.setOffset(0);
            readEPC.setCount(0);
            reader.Actions.TagAccess.readEvent(readEPC, null, null);
        } catch (InvalidUsageException | OperationFailureException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops the current inventory operation on the reader.
     */
    synchronized void stopInventory() {
        // check reader connection
        if (!isReaderConnected())
            return;
        try {
            reader.Actions.Inventory.stop();
        } catch (InvalidUsageException | OperationFailureException e) {
            e.printStackTrace();
        }
    }

    // Read/Status Notify handler
    // Implement the RfidEventsLister class to receive event notifications
    public class EventHandler implements RfidEventsListener {
        // Read Event Notification
        public void eventReadNotify(RfidReadEvents e) {
            // Recommended to use new method getReadTagsEx for better performance in case of large tag population
            TagData[] myTags = reader.Actions.getReadTags(100);
            if (myTags != null) {
                for (int index = 0; index < myTags.length; index++) {
                    Log.d(TAG, "1. Tag ID = " + myTags[index].getTagID());
                    Log.d(TAG, "2. ACCESS code = " + myTags[index].getOpCode());
                    if (myTags[index].getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
                            myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                        if (myTags[index].getMemoryBankData().length() > 0) {
                            Log.d(TAG, "3. Mem Bank = " + myTags[index].getMemoryBank());
                            Log.d(TAG, "4. Mem Bank Data " + myTags[index].getMemoryBankData());
                        }
                    }
                    //////////////////////////////////////////////////////////////
                    // untraceable
                    {
                        if (myTags[index].getG2v2OpStatus() == null) {
                            Log.d(TAG, "5. Untraceable OK: EPC=" + myTags[index].getTagID() +
                                    " ,G2V2 response= " + myTags[index].getG2v2Response() +
                                    " ,op=" + myTags[index].getG2v2OpCode() +
                                    " ,status=" + myTags[index].getG2v2OpStatus() +
                                    " ,opCode=" + myTags[index].getOpCode());
                            Log.d(TAG, "Untraceable Mem Bank Data " + myTags[index].getMemoryBankData());
                            Log.d(TAG, "Untraceable EPC= " + myTags[index].getTagID());
                        } else {
                            Log.d(TAG, "Untraceable ERROR Status=" + myTags[index].getG2v2OpStatus() + " ,response=" + myTags[index].getG2v2Response());
                        }
                    }
                    //////////////////////////////////////////////////////////////
                    // Tag Location
                    if (myTags[index].isContainsLocationInfo()) {
                        short dist = myTags[index].LocationInfo.getRelativeDistance();
                        Log.d(TAG, "Tag relative distance " + dist);
                    }
                }
                // possibly if operation was invoked from async task and still busy
                // handle tag data responses on parallel thread thus THREAD_POOL_EXECUTOR
                new AsyncDataUpdate().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myTags);
            }
        }

        // Status Event Notification
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            Log.d(TAG, "Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            context.handleTriggerPress(true);
                            return null;
                        }
                    }.execute();
                }
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            context.handleTriggerPress(false);
                            return null;
                        }
                    }.execute();
                }
            }
        }
    }

    private class AsyncDataUpdate extends AsyncTask<TagData[], Void, Void> {
        @Override
        protected Void doInBackground(TagData[]... params) {
            context.handleTagdata(params[0]);
            return null;
        }
    }

    interface ResponseHandlerInterface {
        void handleTagdata(TagData[] tagData);

        void handleTriggerPress(boolean pressed);
        //void handleStatusEvents(Events.StatusEventData eventData);
    }

}
