package workflow.services;

import workflow.models.Workflow;

public class ValidationService {
    public boolean validateWorkflow(Workflow workflow) {
        return workflow != null
            && workflow.getId() != null
            && !workflow.getId().isBlank()
            && workflow.getName() != null
            && !workflow.getName().isBlank();
    }
}
