//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.05.02 at 11:26:54 AM CEST 
//


package sodekovs.bikesharing.data.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="DestinationProbability">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="destination" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="probability" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="departureProbability" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="stationID" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "destinationProbability"
})
@XmlRootElement(name = "ProbabilitiesForStation")
public class ProbabilitiesForStation {

    @XmlElement(name = "DestinationProbability", required = true)
    protected ProbabilitiesForStation.DestinationProbability destinationProbability;
    @XmlAttribute(name = "departureProbability", required = true)
    protected double departureProbability;
    @XmlAttribute(name = "stationID", required = true)
    protected String stationID;

    /**
     * Gets the value of the destinationProbability property.
     * 
     * @return
     *     possible object is
     *     {@link ProbabilitiesForStation.DestinationProbability }
     *     
     */
    public ProbabilitiesForStation.DestinationProbability getDestinationProbability() {
        return destinationProbability;
    }

    /**
     * Sets the value of the destinationProbability property.
     * 
     * @param value
     *     allowed object is
     *     {@link ProbabilitiesForStation.DestinationProbability }
     *     
     */
    public void setDestinationProbability(ProbabilitiesForStation.DestinationProbability value) {
        this.destinationProbability = value;
    }

    /**
     * Gets the value of the departureProbability property.
     * 
     */
    public double getDepartureProbability() {
        return departureProbability;
    }

    /**
     * Sets the value of the departureProbability property.
     * 
     */
    public void setDepartureProbability(double value) {
        this.departureProbability = value;
    }

    /**
     * Gets the value of the stationID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStationID() {
        return stationID;
    }

    /**
     * Sets the value of the stationID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStationID(String value) {
        this.stationID = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="destination" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="probability" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class DestinationProbability {

        @XmlAttribute(name = "destination", required = true)
        protected String destination;
        @XmlAttribute(name = "probability", required = true)
        protected double probability;

        /**
         * Gets the value of the destination property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getDestination() {
            return destination;
        }

        /**
         * Sets the value of the destination property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setDestination(String value) {
            this.destination = value;
        }

        /**
         * Gets the value of the probability property.
         * 
         */
        public double getProbability() {
            return probability;
        }

        /**
         * Sets the value of the probability property.
         * 
         */
        public void setProbability(double value) {
            this.probability = value;
        }

    }

}
