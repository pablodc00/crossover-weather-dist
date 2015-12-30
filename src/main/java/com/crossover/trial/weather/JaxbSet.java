package com.crossover.trial.weather;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Set;

/**
 * Created by thiago-rs on 12/30/15.
 */
@XmlRootElement(name="Set")
public class JaxbSet<T>{
    protected Set<T> set;

    public JaxbSet(){}

    public JaxbSet(Set<T> set){
        this.set=set;
    }

    @XmlElement(name="Item")
    public Set<T> getSet(){
        return set;
    }
}