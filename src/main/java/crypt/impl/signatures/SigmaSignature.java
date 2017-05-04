package crypt.impl.signatures;

import javax.xml.bind.annotation.XmlElement;

import model.entity.ElGamalKey;
import model.entity.sigma.Or;
import model.entity.sigma.Responses;

/**
 * @author Nathanaël Eon
 */
public class SigmaSignature {

	/* Element that compose a Sigma Signature */
	@XmlElement(name="pcs")
	private Or pcs;
	@XmlElement(name="rpcs")
	private Responses rpcs;
	
	@XmlElement(name="trentK")
	private ElGamalKey trentK;
	
	/* Simple constructor for Json */
	public SigmaSignature(){}
	
	/* Comstructor with params */
	public SigmaSignature(Or p, Responses r){
		this.pcs = p;
		this.rpcs = r;
	}
	
	public Or getPcs(){
		return pcs;
	}
	public void setPcs(Or p){
		this.pcs = p;
	}
	
	public Responses getRpcs(){
		return rpcs;
	}
	public void setRpcs(Responses r){
		this.rpcs = r;
	}
	
	public ElGamalKey getTrentK(){
		return trentK;
	}
	public void setTrenK(ElGamalKey t){
		this.trentK = t;
	}
}