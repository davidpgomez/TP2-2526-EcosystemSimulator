package simulator.model;

import org.json.JSONObject;

public interface JSONable {
    default public JSONObject asJSON() {
        return new JSONObject();
    }
}
