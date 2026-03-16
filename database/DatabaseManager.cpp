#include <string>

class DatabaseManager {
public:
    bool connect(const std::string& connectionString) {
        (void)connectionString;
        return true;
    }
};
