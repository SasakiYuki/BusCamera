package wacode.yuki.nonoitibuss3.utils

import android.content.Context
import io.realm.Realm
import io.realm.RealmQuery
import io.realm.RealmResults
import wacode.yuki.nonoitibuss3.entity.StrImageRealm

/**
 * Created by Yuki on 2016/04/10.
 */
object RealmUtils {

    fun put(context: Context,place:String,strBase64:String){
        val realm: Realm = Realm.getInstance(context,place)

        realm.beginTransaction()

        var item = realm.createObject(StrImageRealm::class.java)

        item.strBase64 = strBase64

        realm.commitTransaction()
    }

    fun delete(context: Context,place:String,result:RealmResults<StrImageRealm>){
        val realm:Realm = Realm.getInstance(context,place)

        realm.beginTransaction()
        result.clear()

        realm.commitTransaction()
    }

    fun query(context: Context,place: String,item:String):RealmResults<StrImageRealm>{
        val realm: Realm = Realm.getInstance(context,place)

        realm.beginTransaction()
        val query = realm.where(StrImageRealm::class.java)

        query.equalTo("strBase64",item)

        val result = query.findAll()

        realm.commitTransaction()
        return result
    }

    fun getAll(context: Context,place: String):RealmResults<StrImageRealm>{
        val realm:Realm = Realm.getInstance(context,place)
        val query: RealmQuery<StrImageRealm> = realm.where(StrImageRealm::class.java)

        val result:RealmResults<StrImageRealm> = query.findAll()

        return result
    }
}