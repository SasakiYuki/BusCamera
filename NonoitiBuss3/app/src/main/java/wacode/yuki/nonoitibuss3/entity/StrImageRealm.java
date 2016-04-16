package wacode.yuki.nonoitibuss3.entity;

import io.realm.RealmObject;

/**
 * Created by Yuki on 2016/04/10.
 */
public class StrImageRealm extends RealmObject{
    private String strBase64;

    public String getStrBase64() {
        return strBase64;
    }

    public void setStrBase64(String strBase64) {
        this.strBase64 = strBase64;
    }
}
