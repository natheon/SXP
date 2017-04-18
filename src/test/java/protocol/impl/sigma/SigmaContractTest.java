package protocol.impl.sigma;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.core.type.TypeReference;

import controller.Users;
import controller.tools.JsonTools;
import crypt.api.signatures.Signable;
import crypt.factories.ElGamalAsymKeyFactory;
import crypt.impl.signatures.ElGamalSignature;
import crypt.impl.signatures.ElGamalSigner;
import model.api.Status;
import model.api.Wish;
import model.entity.ContractEntity;
import model.entity.ElGamalKey;
import model.entity.User;
import protocol.impl.sigma.SigmaContract;
import util.TestInputGenerator;

/**
 * ElGamalContract unit test
 * @author denis.arrivault[@]univ-amu.fr
 * @author nathanael.eon[@]lif.univ-mrs.fr
 *
 */
public class SigmaContractTest {
	@Rule public ExpectedException exception = ExpectedException.none();

	class Clauses implements Signable<ElGamalSignature> {
		private ElGamalSignature sign;
		private String text;

		public Clauses(String text) {
			this.text = text;
		}

		@Override
		public byte[] getHashableData() {
			return text.getBytes();
		}

		@Override
		public void setSign(ElGamalSignature s) {
			this.sign = s;
		}

		@Override
		public ElGamalSignature getSign() {
			return this.sign;
		}
	}
	
	private final int N = TestInputGenerator.getRandomInt(1, 20);
	private SigmaContract contract;
	private SigmaContract contract2;
	private ContractEntity contractE;
	private String text;
	private Clauses clauses;
	private ArrayList<String> cl = new ArrayList<String>();

	@Before
	public void instantiate(){
		text = TestInputGenerator.getRandomIpsumText();
		clauses = new Clauses(text);
		contract = new SigmaContract(clauses);
		contractE = new ContractEntity();
		contractE.setParties(new ArrayList<String>());
		contractE.setSignatures(new HashMap<String,String>());
		contractE.setClauses(new ArrayList<String>());
	}

	@Test
	public void clausesGetterTest(){
		contract2 = new SigmaContract(contractE);
		contract2.setClauses(clauses);
		assertArrayEquals(contract2.getClauses().getHashableData(), clauses.getHashableData());
		contract2.setClauses(cl);
		contract.setClauses(cl);
		assertArrayEquals(contract2.getClauses().getHashableData(), contract.getClauses().getHashableData());
	}

	@Test
	public void setPartiesTest(){
		JsonTools<Collection<User>> json = new JsonTools<>(new TypeReference<Collection<User>>(){});
		Users users = new Users();
		Collection<User> u = json.toEntity(users.get());
		ArrayList<String> ids = new ArrayList<String>();
		ArrayList<ElGamalKey> keys = new ArrayList<ElGamalKey>(); 
		for (User user : u){
			ids.add(user.getId());
			keys.add(user.getKey());
		}
		contract.setParties(ids);
		assertTrue(contract.getParties().toString().equals(keys.toString()));
	}
	
	@Test
	public void addSignatureExceptionTest1(){
		exception.expect(RuntimeException.class);
		exception.expectMessage("invalid key");
		ElGamalKey key = ElGamalAsymKeyFactory.create(false);
		ElGamalSigner signer = new ElGamalSigner();
		signer.setKey(key);
		contract.addSignature(key, contract.sign(signer, null));
	}

	@Test
	public void addSignatureExceptionTest2(){
		exception.expect(RuntimeException.class);
		exception.expectMessage("invalid key");
		ElGamalKey key = ElGamalAsymKeyFactory.create(false);
		ElGamalSigner signer = new ElGamalSigner();
		signer.setKey(key);
		contract.addSignature(null, contract.sign(signer, null));
	}

	@Test
	public void badFinalizationTest(){
		ArrayList<ElGamalKey> parties = new ArrayList<ElGamalKey>();
		for(int i = 0; i<N; i++){
			ElGamalKey key = ElGamalAsymKeyFactory.create(false);
			parties.add(key);
		}
		contract.setParties(parties, true);
		assertFalse(contract.isFinalized());
		for(ElGamalKey key : contract.getParties()){
			assertTrue(key.getClass().getName().equals("model.entity.ElGamalKey"));
			ElGamalSigner signer = new ElGamalSigner();
			signer.setKey(ElGamalAsymKeyFactory.create(false));
			contract.addSignature(key, contract.sign(signer, null));
		}
		assertFalse(contract.isFinalized());
		assertFalse(contract.checkContrat(contract));
		assertFalse(contract.checkContrat(new SigmaContract(new Clauses(TestInputGenerator.getRandomIpsumText()))));
	}

	@Test
	public void finalizedTest(){
		ArrayList<ElGamalKey> parties = new ArrayList<ElGamalKey>();
		for(int i = 0; i<N; i++){
			ElGamalKey key = ElGamalAsymKeyFactory.create(false);
			parties.add(key);
		}
		contract.setParties(parties, true);
		for(ElGamalKey key : contract.getParties()){
			assertTrue(key.getClass().getName().equals("model.entity.ElGamalKey"));
			ElGamalSigner signer = new ElGamalSigner();
			signer.setKey(key);
			contract.addSignature(key, contract.sign(signer, null));
		}
		assertTrue(contract.isFinalized());
		assertTrue(contract.checkContrat(contract));
		assertFalse(contract.checkContrat(new SigmaContract(new Clauses(TestInputGenerator.getRandomIpsumText()))));
	}

	@Test
	public void getSetWishTest(){
		contract.setWish(Wish.ACCEPT);
		assertTrue(contract.getWish().compareTo(Wish.ACCEPT) == 0);
	}
	
	@Test
	public void getSetStatusTest(){
		contract.setStatus(Status.NOWHERE);
		assertTrue(contract.getStatus().compareTo(Status.NOWHERE) == 0);
	}
}