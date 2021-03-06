package com.example.medication_reminder_android_app.SQLiteDB;

import android.app.Application;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.renderscript.ScriptGroup;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * @author Hayley Roberts
 * @lastModified 3/22/2021 by Hayley Roberts
 */


public class DatabaseRepository {

    private final DataAccessObject dao;
    private final AppDatabase db;

    private MedicationEntity singleMed;
    private ReminderEntity singleReminder;
    private long lastMedPk = 0; // keep track of medication primary keys. Use bc pk auto-incr
    private long lastReminderPk = 0; //leep track of last reminder pk. Use bc pk auto-incr

    public DatabaseRepository(Application application){
        db = AppDatabase.getDatabase(application);
        dao = db.dataAccessObject();
    }

    private void getMedByNameAsyncFinished(MedicationEntity m) { singleMed = m; }
    private void getReminderAsyncFinished(ReminderEntity r) { singleReminder = r; }
    private void insertMedAsyncFinished(long medPk) { lastMedPk = medPk; }
    private void insertReminderAsyncFinished(long reminderPk) { lastReminderPk = reminderPk; }

    //TODO: finish this: tags need to be added to Med on input
//    public LiveData<List<MedicationEntity>> filterMedications(String[] tags){
//        //because want to pull if have any tags, need to build the query WHERE clause as such
//        String queryReq = ""; //to send to query so can get tags in any order
//        if(tags.length > 1){
//            for(String tag: tags){
//                queryReq += " tags LIKE %" + tag + "% OR";
//            }
//            if(queryReq.length() > 4){
//                queryReq = queryReq.substring(0, queryReq.length() - 4); //Remove last OR
//            }
//        } else{
//            queryReq = "tags LIKE %%";
//        }
//
//        return dao.loadFilteredMedications(queryReq);
//    }


    /**
     * @author Hayley Roberts
     * returns list of all Medications wrapped in live data for UI
     * @return
     */
    public LiveData<List<MedicationEntity>> getAllMeds(){
        LiveData<List<MedicationEntity>> blah =  dao.getAllMeds();
        if(blah == null) Log.d("app-debug", "blah is null");
        else if(blah.getValue() == null) Log.d("app-debug", "blah.getValue is null");
        else Log.d("app-debug", "Blah is gotten size: " + blah.getValue().size());
        return blah;
    }

    /**
     * @author Hayley Roberts
     * loads a number of reminders from reminders table
     * @param numOfReminders
     * @return
     */
    public Single<ReminderEntity[]> getReminders(int numOfReminders){
        return dao.selectNextReminders(numOfReminders);
    }

    /**
     * @author Hayley Roberts
     * grabs medicationEntity by medName from MedTable
     * @param medName
     * @return
     */
    public Single<MedicationEntity> getMedByName(String medName){
        return dao.getMedicationByName(medName);
    }

    /**
     * @author Hayley Roberts
     * grabs MedicationEntity from MedTable by Primary Key
     * @param entityId
     * @return
     */
    public Single<MedicationEntity> getMedById(long entityId){
        return dao.getMedicationById(entityId);
    }

    /**
     * @author Hayley Roberts
     * grabs ReminderEntity from ReminderTable by primary key
     * @param entityId
     * @return
     */
    public Single<ReminderEntity> getReminderById(long entityId){
        return dao.getReminder(entityId);
    }

    /**
     *@author Hayley Roberts
     * Insert Medication into MedicationTable
     * @param m
     */
    public void insertMed(MedicationEntity m){
        new AsyncInsertMedication(dao, this).execute(m);
    }

    /**
     * @author Hayley Roberts
     * insert Reminder into ReminderTable
     * @param r
     */
    public void insertReminder(ReminderEntity r){
        new AsyncInsertReminder(dao, this).execute(r);
    }

    /**
     * @author Hayley Roberts
     * Insert a medication and reminder into the respective tables
     * @param m
     */
    public void insertMedAndReminder(MedicationEntity m){
        new AsyncInsertMedAndReminder(dao).execute(m);
    }

    /**
     * @author Hayley Roberts
     * Update the acknowledgements in the Medication table for a medication entity
     * @param m
     * @param acknowledgementList
     */
    public void updateAcknowledgements(MedicationEntity m, String acknowledgementList) {
        new AsyncUpdateAcknowledgement(dao, m).execute(acknowledgementList);
    }

    /**
     * @author Hayley Roberts
     * Add a reminder ID to a medication entity
     * @param m
     * @param reminderPK
     */
    public void addReminderID(MedicationEntity m, long reminderPK){ new AsyncAddReminderID(dao, m).execute(reminderPK); }

    /**
     * @author Hayley Roberts
     * update the ReminderEntity with next date and time.
     * Date should be formatted as YYYY-MM-DD, and time should be formatted as HH:MM
     * @param r
     * @param date
     * @param time
     * @param timeIntervalIndex
     */
    public void updateReminderDateAndTime(ReminderEntity r, String date, String time, int timeIntervalIndex){
        String dateTime = date + " " + time + " " + timeIntervalIndex;
        new AsyncReminderUpdateDateTime(dao, r).execute(dateTime);
    }

    /**
     * @author Hayley Roberts
     * Delete medication from Medication table
     * @param m
     */
    public void deleteMed(MedicationEntity m){
        new AsyncDeleteMedication(dao).execute(m);
    }

    /**
     * @author Hayley Roberts
     * Delete medication from Medication table
     * @param medName
     */
    public void deleteMedByName(String medName) { new AsyncDeleteMedByName(dao).execute(medName); }

    /**
     * @author Hayley Roberts
     * Delete Reminder from reminderTable
     * @param r
     */
    public void deleteReminder(ReminderEntity r){
        new AsyncDeleteReminder(dao).execute(r);
    }

    /**
     * @author Hayley Roberts
     * Delete reminder from ReminderTable
     * @param reminderId
     */
    public void deleteReminderById(long reminderId) { new AsyncDeleteReminderById(dao).execute(reminderId); }

    /**
     * @author Hayley Roberts
     * Delete all reminders that are of Medication Classification
     */
    public void deleteAllMedReminders() { new AsyncDeleteAllMedReminders(dao).execute(); }

    /**
     * @author Hayley Roberts
     * Delete all reminders of Appointment classification
     */
    public void deleteAllApptReminders() { new AsyncDeleteAllApptReminders(dao).execute(); }

    /**
     * @author Hayley Roberts
     * Delete all medications from MedicationTable
     */
    public void deleteAllMeds(){
        new AsyncDeleteAllMeds(dao).execute();
    }

    /**
     * @author Hayley Roberts
     * Delete all reminders from ReminderTable
     */
    public void deleteAllReminders(){
        new AsyncDeleteAllReminders(dao).execute();
    }


    /*
    All of the classes below are AsyncTasks that handle calling dao methods in a separate thread from the Main thread
     */

    private static class AsyncInsertMedication extends AsyncTask<MedicationEntity, Void, Long>{
        private final DataAccessObject dao;
        private final DatabaseRepository delegate;

        public AsyncInsertMedication(DataAccessObject inputHandler, DatabaseRepository repo){
            dao = inputHandler;
            delegate = repo;
        }

        @Override
        protected Long doInBackground(MedicationEntity... params){
            Long pk = dao.insertMedication(params[0]);
            params[0].setPrimaryKey(pk);
            return pk;
        }

        @Override
        protected void onPostExecute(Long pk){
            delegate.insertMedAsyncFinished(pk);
        }

    }

    private static class AsyncInsertReminder extends AsyncTask<ReminderEntity, Void, Long>{
        private final DataAccessObject dao;
        private final DatabaseRepository delegate;

        public AsyncInsertReminder(DataAccessObject inputHandler, DatabaseRepository repo){
            dao = inputHandler;
            delegate = repo;
        }

        @Override
        protected Long doInBackground(ReminderEntity... reminders){
            Long pk = dao.insertReminder(reminders[0]);
            reminders[0].setPrimaryKey(pk);
            return pk;
        }

        @Override
        protected void onPostExecute(Long pk){
            delegate.insertReminderAsyncFinished(pk);
        }
    }

    private static class AsyncInsertMedAndReminder extends AsyncTask<MedicationEntity, Void, Void> {
        private DataAccessObject dao;

        public AsyncInsertMedAndReminder(DataAccessObject d){
            dao = d;
        }

        @Override
        protected Void doInBackground(MedicationEntity... m){
            long medPk = dao.insertMedication(m[0]);
            String[] sepDate = m[0].getFirstDate().split(" ");
            ReminderEntity reminder = new ReminderEntity("M", sepDate[1], sepDate[0], 0, medPk);
            long reminderPk = dao.insertReminder(reminder);
            dao.addReminderID(medPk, reminderPk);
            return null;
        }

    }

    private static class AsyncUpdateAcknowledgement extends AsyncTask<String, Void, Void>{
        private final DataAccessObject dao;
        private final MedicationEntity m;

        public AsyncUpdateAcknowledgement(DataAccessObject inputHandler, MedicationEntity medEntity){
            dao = inputHandler;
            m = medEntity;
        }

        @Override
        protected Void doInBackground(String... params){
            dao.updateAcknowledgements(m.getPrimaryKey(), params[0]);
            return null;
        }

    }

    private static class AsyncAddReminderID extends AsyncTask<Long, Void, Void>{
        private final DataAccessObject dao;
        private final MedicationEntity m;

        public AsyncAddReminderID(DataAccessObject d, MedicationEntity medEntity){
            dao = d;
            m = medEntity;
        }

        @Override
        protected Void doInBackground(Long... params){
            dao.addReminderID(m.getPrimaryKey(), params[0]);
            return null;
        }
    }

    private static class AsyncReminderUpdateDateTime extends AsyncTask<String, Void, Void>{
        private final DataAccessObject dao;
        private final ReminderEntity r;

        public AsyncReminderUpdateDateTime(DataAccessObject d, ReminderEntity reminderEntity){
            dao = d;
            r = reminderEntity;
        }

        @Override
        protected Void doInBackground(String... params){
            String[] dateTime = params[0].split(" ");
            dao.updateDateAndTime(r.getPrimaryKey(), dateTime[0], dateTime[1], Integer.parseInt(dateTime[2]));
            return null;
        }

    }

    private static class AsyncDeleteMedication extends AsyncTask<MedicationEntity, Void, Void>{
        private final DataAccessObject dao;

        public AsyncDeleteMedication(DataAccessObject inputHandler){
            dao = inputHandler;
        }

        @Override
        protected Void doInBackground(MedicationEntity... meds){
            dao.deleteMedication(meds[0]);
            return null;
        }
    }

    private static class AsyncDeleteMedByName extends AsyncTask<String, Void, Void>{
        private final DataAccessObject dao;

        public AsyncDeleteMedByName(DataAccessObject d){ dao = d; }

        @Override
        protected Void doInBackground(String... params){
            dao.deleteMedicationByName(params[0]);
            return null;
        }
    }

    private static class AsyncDeleteReminder extends AsyncTask<ReminderEntity, Void, Void>{
        private final DataAccessObject dao;

        public AsyncDeleteReminder(DataAccessObject inputHandler){
            dao = inputHandler;
        }

        @Override
        protected Void doInBackground(ReminderEntity... reminders){
            dao.deleteReminder(reminders[0]);
            return null;
        }
    }

    private static class AsyncDeleteReminderById extends AsyncTask<Long, Void, Void>{
        private final DataAccessObject dao;

        public AsyncDeleteReminderById(DataAccessObject inputHandler){
            dao = inputHandler;
        }

        @Override
        protected Void doInBackground(Long... params){
            dao.deleteReminderById(params[0]);
            return null;
        }
    }

    private static class AsyncDeleteAllMedReminders extends AsyncTask<Void, Void, Void>{
        private final DataAccessObject dao;

        public AsyncDeleteAllMedReminders(DataAccessObject d){
            dao = d;
        }

        @Override
        protected Void doInBackground(Void... params){
            dao.deleteAllMedicationReminders();
            return null;
        }
    }

    private static class AsyncDeleteAllApptReminders extends AsyncTask<Void, Void, Void>{
        private final DataAccessObject dao;

        public AsyncDeleteAllApptReminders(DataAccessObject d){
            dao = d;
        }

        @Override
        protected Void doInBackground(Void... params){
            dao.deleteAllAppointmentReminders();
            return null;
        }
    }


    private static class AsyncDeleteAllMeds extends AsyncTask<Void, Void, Void>{
        private final DataAccessObject dao;

        public AsyncDeleteAllMeds(DataAccessObject inputHandler){
            dao = inputHandler;
        }

        @Override
        protected Void doInBackground(Void... voids){
            dao.clearAllMedications();
            return null;
        }
    }

    private static class AsyncDeleteAllReminders extends AsyncTask<Void, Void, Void>{
        private final DataAccessObject dao;

        public AsyncDeleteAllReminders(DataAccessObject inputHandler){
            dao = inputHandler;
        }

        @Override
        protected Void doInBackground(Void... voids){
            dao.clearAllReminders();
            return null;
        }
    }

}
