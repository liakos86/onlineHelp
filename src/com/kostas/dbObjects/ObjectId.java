package com.kostas.dbObjects;

/**
 * Created by KLiakopoulos on 4/23/2015.
 */
public class ObjectId {
    private String $oid;

    public ObjectId(String $oid){
        this.$oid = $oid;
    }

    public String get$oid() {
        return $oid;
    }

    public void set$oid(String $oid) {
        this.$oid = $oid;
    }
}
