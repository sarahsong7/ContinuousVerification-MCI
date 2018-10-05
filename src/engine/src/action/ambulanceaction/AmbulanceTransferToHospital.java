package action.ambulanceaction;

import agents.Ambulance;
import agents.Hospital;
import agents.Patient;

public class AmbulanceTransferToHospital extends AmbulanceAction {

    Hospital hospital;
    Patient patient;

    public AmbulanceTransferToHospital(Ambulance target, Hospital hospital, Patient targetPatient) {
        super(target);

        this.hospital = hospital;
        this.patient = targetPatient;
    }

    @Override
    public void onUpdate() {
        ambulance.moveTo(hospital.position);
        if(ambulance.isArrivedAt(hospital.position)) {
            hospital.hospitalize(patient);
            ambulance.changeAction(new AmbulanceFree(ambulance));
        }
    }
}