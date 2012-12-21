package org.openmrs.module.patientregistration.controller.workflow;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientregistration.PatientRegistrationConstants;
import org.openmrs.module.patientregistration.PatientRegistrationGlobalProperties;
import org.openmrs.module.patientregistration.service.PatientRegistrationService;
import org.openmrs.module.patientregistration.util.PatientRegistrationWebUtil;
import org.openmrs.module.patientregistration.util.UserActivityLogger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PrintRegistrationLabelController {

    @RequestMapping("/module/patientregistration/workflow/printRegistrationLabel.form") 
	public ModelAndView showPrintRegistrationLabel(HttpSession session, ModelMap model, @RequestParam(value = "patientId", required = false) Integer patientId,
	                                               										@RequestParam (value = "count", required = false) Integer count, 
	                                               										@RequestParam (value = "nextTask", required = false) String nextTask, 
	                                               										@RequestParam (value = "identifierTypeId", required = false) Integer identifierTypeId) {
	
		if (!PatientRegistrationWebUtil.confirmActivePatientRegistrationSession(session)) {
			return new ModelAndView(PatientRegistrationConstants.WORKFLOW_FIRST_PAGE);
		}
		
		Patient patient = PatientRegistrationWebUtil.updatePatientIdSessionAttributeAndGetPatient(session, patientId);
		
		// figure out how many labels are configured to be printed
		// if the number of labels to count hasn't been specified as a parameter, use the default specified in the global property 
		if (count == null) {
			count = PatientRegistrationGlobalProperties.GLOBAL_PROPERTY_REGISTRATION_LABEL_PRINT_COUNT();
		}
		if (count == null) {
			// default to 1
			count = 1;
		}
		if(count>0){
			if(identifierTypeId!=null){			
				PatientIdentifierType patientIdentifierType= Context.getPatientService().getPatientIdentifierType(identifierTypeId);		
				PatientIdentifierType globalDentalType = PatientRegistrationGlobalProperties.GLOBAL_PROPERTY_DENTAL_DOSSIER();
				if(globalDentalType!=null 
						&& patientIdentifierType!=null 
						&& globalDentalType.getId().compareTo(patientIdentifierType.getId())==0){
					Context.getService(PatientRegistrationService.class).printDentalDossierLabel(patient, PatientRegistrationWebUtil.getRegistrationLocation(session), 1);
				}
			}else{
				// print the registration label (or labels)
				boolean labelSuccess = Context.getService(PatientRegistrationService.class).printRegistrationLabel(patient, PatientRegistrationWebUtil.getRegistrationLocation(session), count);
				if (labelSuccess) {
					UserActivityLogger.logActivity(session, PatientRegistrationConstants.ACTIVITY_DOSSIER_LABEL_PRINTING_SUCCESSFUL);
				}
				else {
					UserActivityLogger.logActivity(session, PatientRegistrationConstants.ACTIVITY_DOSSIER_LABEL_PRINTING_FAILED);
					// TODO: Decide what else to do if this fails
				}
				
				// print out the ID card label
				boolean cardSuccess = Context.getService(PatientRegistrationService.class).printIDCardLabel(patient);
				if (cardSuccess) {
					UserActivityLogger.logActivity(session, PatientRegistrationConstants.ACTIVITY_ID_CARD_LABEL_PRINTING_SUCCESSFUL);
				}
				else {
					UserActivityLogger.logActivity(session, PatientRegistrationConstants.ACTIVITY_ID_CARD_LABEL_PRINTING_FAILED);
					// TODO: Decide what else to do if this fails
				}
			}
		}
		String nextPage = "redirect:/module/patientregistration/workflow/patientDashboard.form?patientId="+ patientId;
		if(StringUtils.isNotBlank(nextTask)){
			nextPage = nextPage + "&nextTask=" + nextTask;
		}
		return new ModelAndView(nextPage);
	}
}
