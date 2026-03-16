#include <string>

class ValidationService {
public:
    bool validateWorkflow(const std::string& workflowId) {
        (void)workflowId;
        return true;
    }
};
