package com.emyyn.riley.setmedication;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.joda.time.LocalTime;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.DurationDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.QuantityDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.composite.SimpleQuantityDt;
import ca.uhn.fhir.model.dstu2.composite.TimingDt;
import ca.uhn.fhir.model.dstu2.resource.Medication;
import ca.uhn.fhir.model.dstu2.resource.MedicationOrder;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.valueset.BundleTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.HTTPVerbEnum;
import ca.uhn.fhir.model.dstu2.valueset.MedicationOrderStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.ObservationStatusEnum;
import ca.uhn.fhir.model.dstu2.valueset.UnitsOfTimeEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        load();
        Log.i("Loading....", "Complete");
    }

    public void load() {
        new AsyncTask<Void, Void, Integer>() {
            //Log.i("Loading....", "");
            @Override
            protected Integer doInBackground(Void... params) {
                Log.i("Loading....", "");
                // Create a patient object
                Patient patient = new Patient();
                List<String> ids = new ArrayList<String>(6);
                ids.add("5149");
                ids.add("5401");
                ids.add("5413");
                ids.add("5425");
                ids.add("5437");
                ids.add("5476");

                Observation observation = new Observation();
                observation.setStatus(ObservationStatusEnum.FINAL);
                observation
                        .getCode()
                        .addCoding()
                        .setSystem("http://loinc.org")
                        .setCode("789-8")
                        .setDisplay("Body Temperature- Oral");
                observation.setValue(
                        new QuantityDt()
                                .setValue(98.4)
                                .setUnit("degrees")
                                .setSystem("http://unitsofmeasure.org")
                                .setCode("F"));
                observation.setSubject(new ResourceReferenceDt(patient.getId().getValue()));


                ca.uhn.fhir.model.dstu2.resource.Bundle bundle = new ca.uhn.fhir.model.dstu2.resource.Bundle();
                bundle.setType(BundleTypeEnum.TRANSACTION);
                for (int i = 0; i < ids.size(); i++) {
                    //Create New Medication Order
                    DurationDt d30 = (DurationDt) new DurationDt().setUnit("day").setCode("d").setValue(30.0);
                    LocalTime start = new LocalTime();
                    LocalTime end = start.plus(Period.years(1));
                    List<MedicationOrder.DosageInstruction> doseageInstructions = new ArrayList<MedicationOrder.DosageInstruction>();
                    MedicationOrder.DosageInstruction dosageInstruction = new MedicationOrder.DosageInstruction();
                    dosageInstruction.setText("Take 1 tablet (100 mg total) by mouth 3 (three) times a day before meals")
                            .setMethod(new CodeableConceptDt().setText("Take"))
                            .setTiming(new TimingDt()
                                    .setRepeat(new TimingDt.Repeat()
                                            .setFrequency(1)
                                            .setPeriod(8)
                                            .setPeriodUnits(UnitsOfTimeEnum.H)
                                    ));
                    doseageInstructions.add(dosageInstruction);
                    Medication medication = new Medication();
                    medication.setId("5462245");

                    MedicationOrder medOrder = new MedicationOrder();
                    medOrder.setPatient(new ResourceReferenceDt("Patient/" + ids.get(i)))
                            .setDispenseRequest(new MedicationOrder.DispenseRequest()
                                    .setExpectedSupplyDuration(d30)
                                    .setValidityPeriod(new PeriodDt()
                                            .setEnd(new DateTimeDt(end.toDateTimeToday().toDate()))
                                            .setStart(new DateTimeDt(start.toDateTimeToday().toDate())))
                                    .setQuantity(new SimpleQuantityDt(90.0))

                            )
                            .setDosageInstruction(doseageInstructions)
                            .setStatus(MedicationOrderStatusEnum.ACTIVE)
                            .setMedication(new ResourceReferenceDt(String.valueOf("Medication/23300")).setDisplay("ZYLOPRIM"))
                            .setId(new IdDt(6545461 + i))
                    ;

// The observation refers to the patient using the ID, which is already
// set to a temporary UUID
                    //observation.setSubject(new ResourceReferenceDt(patient.getId().getValue()));
                    //medOrder.setPatient(new ResourceReferenceDt(patient.getId().getValue()));

// Create a bundle that will be used as a transaction


// Add the patient as an entry. This entry is a POST with an
// If-None-Exist header (conditional create) meaning that it
// will only be created if there isn't already a Patient with
// the identifier 12345
                    bundle.addEntry()
                            .setResource(medOrder)
                            .getRequest()
                            .setUrl("MedicationOrder")
                            .setMethod(HTTPVerbEnum.POST);
                }
// Log the request
                FhirContext ctx = FhirContext.forDstu2();
                Log.i("Request", ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(bundle));

// Create a client and post the transaction to the server
                IGenericClient client = ctx.newRestfulGenericClient("http://fhirtest.uhn.ca/baseDstu2");
                ca.uhn.fhir.model.dstu2.resource.Bundle resp = client.transaction().withBundle(bundle).execute();

// Log the response
                Log.i("Response", ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(resp));
                ca.uhn.fhir.model.dstu2.resource.Bundle dstu2Bundle = client.search().forResource(MedicationOrder.class)
                        .where(new StringClientParam("patient._id").matches().value("5476"))
                        .where(new TokenClientParam("status").exactly().code("active"))
                        .include(new Include("MedicationOrder:medication"))
                        .include(new Include("MedicationOrder:patient"))
                        .returnBundle(ca.uhn.fhir.model.dstu2.resource.Bundle.class)
                        .execute();
                Log.i("Before Return", dstu2Bundle.getEntry().get(0).getResource().getResourceName());
//dstu2Bundle.getEntry().get(0).getResource().getResourceName();
                for (int i = 0; i < dstu2Bundle.getEntry().size(); i++) {
                    ca.uhn.fhir.model.dstu2.resource.Bundle.Entry entry = dstu2Bundle.getEntry().get(i);
                    try {
                        Log.i("Entry", "yes please");
                        if (entry.getResource() instanceof MedicationOrder) {
                            MedicationOrder medicationOrder = (MedicationOrder) entry.getResource();
                            MedicationOrder.DosageInstruction firstDosage = medicationOrder.getDosageInstructionFirstRep();
                            Log.i("DosageInstructions", firstDosage.getText());
                            notNull(firstDosage.getRoute().getText(), "Route");
                            notNull(firstDosage.getMethod().getText(), "Method");
                            notNull(firstDosage.getTiming().getRepeat().getFrequency().toString(), "Frequency");
                            notNull(firstDosage.getTiming().getRepeat().getPeriod().toString(), "Period");
                            notNull(firstDosage.getTiming().getRepeat().getPeriodUnits().toString(), "Units");
                            notNull(medicationOrder.getDispenseRequest().getValidityPeriod().getStart().toString(), "Start");
                            notNull(medicationOrder.getDispenseRequest().getValidityPeriod().getEnd().toString(), "End");
                            notNull(medicationOrder.getDispenseRequest().getQuantity().getValue().toString(), "Q");
                            }
                            // + firstDosage.getRoute() + firstDosage.getText());
                        else if (entry.getResource() instanceof Patient) {
                            Patient p = (Patient) entry.getResource();
                            Log.i("Patient", p.getId().getIdPart() + p.getNameFirstRep().getFamilyFirstRep() + " , " + p.getNameFirstRep().getGivenFirstRep());
                        } else {
                            Log.i("Exit Else", "Tnothingsasdf");
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }

                Log.i("Exit Return", "Tingsasdf");
                return 0;
            }
        }.execute();
    }

    public Boolean notNull(String s, String title) {
        if (s != null) {
            Log.i(title+" ", s);
            return true;
        } else {
            Log.i("Null", title);
            return false;
        }
    }
}

