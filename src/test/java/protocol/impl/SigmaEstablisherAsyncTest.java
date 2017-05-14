package protocol.impl;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import controller.Application;
import controller.ApplicationForTests;
import crypt.api.hashs.Hasher;
import crypt.factories.ElGamalAsymKeyFactory;
import crypt.factories.HasherFactory;
import model.api.Status;
import model.api.SyncManager;
import model.entity.ContractEntity;
import model.entity.ElGamalKey;
import model.entity.User;
import model.syncManager.UserSyncManagerImpl;
import network.api.EstablisherService;
import protocol.impl.sigma.SigmaContract;
import protocol.impl.sigma.TrentAsync;
import util.TestInputGenerator;
import util.TestUtils;

public class SigmaEstablisherAsyncTest {
	public static Application application;
	public static ApplicationForTests applicationForTests;
	public static final int restPort = 5601;
	public static final int restPort2 = 5602;
	
	public static final int N = 2;
	
	// Users
	private static User[] u;
	private static ElGamalKey[] keysR;
	private static ElGamalKey trentK = ElGamalAsymKeyFactory.create(false);

	// A contract for each signer
	private static SigmaContract[] c;
	// A contract entity
	private static ContractEntity[] ce;
	

	/*
	 * Create the users, the application
	 */
	@Before
	public void initialize(){
		application = new Application();
		application.runForTests(restPort);

		applicationForTests = new ApplicationForTests();
		applicationForTests.runForTests(restPort2);
		
		// Initialize the users
		u = new User[N];
		for (int k=0; k<N; k++){
			String login = TestInputGenerator.getRandomAlphaWord(20);
			String password = TestInputGenerator.getRandomPwd(20);
			
			u[k] = new User();
			u[k].setNick(login);
			Hasher hasher = HasherFactory.createDefaultHasher();
			u[k].setSalt(HasherFactory.generateSalt());
			hasher.setSalt(u[k].getSalt());
			u[k].setPasswordHash(hasher.getHash(password.getBytes()));
			u[k].setCreatedAt(new Date());
			u[k].setKey(ElGamalAsymKeyFactory.create(false));
			SyncManager<User> em = new UserSyncManagerImpl();
			em.begin();
			em.persist(u[k]);
			em.end();
		}
		
		// Initialize the keys
		ElGamalKey key;
		keysR = new ElGamalKey[N];		
		for (int k=0; k<N; k++){
			key = u[k].getKey();
			keysR[k] = new ElGamalKey();
			keysR[k].setG(key.getG());
			keysR[k].setP(key.getP());
			keysR[k].setPublicKey(key.getPublicKey());
		}
		
		c = new SigmaContract[N];
		ce = new ContractEntity[N];
		
		// Initialize the contracts 
		ArrayList<String> cl = new ArrayList<String>();
		cl.add(TestInputGenerator.getRandomIpsumText());
		cl.add(TestInputGenerator.getRandomIpsumText());
		

		ArrayList<String> parties = new ArrayList<String>();
		for(int i = 0; i<u.length; i++){
			parties.add(u[i].getId());
		}

		for (int k=0; k<N; k++){
			ce[k] = new ContractEntity();
			ce[k].setParties(parties);
			ce[k].setClauses(cl);
			ce[k].setSignatures(new HashMap<String, String>());
			c[k] = new SigmaContract(ce[k]);
		}
	}

	@After
	public void deleteBaseAndPeer(){
		TestUtils.removeRecursively(new File(".db-" + restPort + "/"));
		TestUtils.removeRecursively(new File(".db-" + restPort2 + "/"));
		TestUtils.removePeerCache();
		application.stop();
		applicationForTests.stop();
	}
	
	// Test a simple signing protocol
	//@Test
	public void TestA(){
		SigmaEstablisherAsync[] sigmaE = new SigmaEstablisherAsync[N];
		for (int k=0; k<N; k++){
				sigmaE[k] = new SigmaEstablisherAsync(u[k].getKey(), trentK);
		}
		sigmaE[0].establisherService =(EstablisherService) ApplicationForTests.getInstance().getPeer().getService(EstablisherService.NAME);
		sigmaE[0].peer = ApplicationForTests.getInstance().getPeer();
		
		for (int k=0; k<N; k++){
			sigmaE[k].initialize(c[k]);
			sigmaE[k].start();
		}
		
		// Time to realize procedure
		try{
			Thread.sleep(5000);
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		boolean res = true;
		for (int k=0; k<N; k++){
			res =  res && c[k].isFinalized();
		}
		
		assertTrue(res);
	}

	// resolveInitiater, limit is the failing round
	public void resolveInitiator(int limit){
		
		new TrentAsync(trentK);
		
		SigmaEstablisherAsync[] sigmaE = new SigmaEstablisherAsync[N];
		
		for (int k=1; k<N; k ++)
			sigmaE[k] = new SigmaEstablisherAsync(u[k].getKey(), trentK);
		
		sigmaE[0] = new SigmaEstablisherAsyncFailer(u[0].getKey(), trentK, limit, false);
		sigmaE[0].establisherService =(EstablisherService) ApplicationForTests.getInstance().getPeer().getService(EstablisherService.NAME);
		sigmaE[0].peer = ApplicationForTests.getInstance().getPeer();
		
		for (int k=0; k<N; k++){
			sigmaE[k].initialize(c[k]);
			sigmaE[k].start();
		}
		
		try{
			Thread.sleep(3000);
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
 
	// Test an abort in protocol (Trent doesn't give the signature)
	@Test
	public void TestB(){
		resolveInitiator(1);
		
		// Time to realize procedure
		for (int k=0; k<5; k++){
			try{
				Thread.sleep(1000);
			}catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
		boolean res = true;
		for (int k=0; k<N; k++){
			res =  res && c[k].isFinalized();
			System.out.println(k + " : " + c[k].getStatus().equals(Status.CANCELLED));
			assertTrue(c[k].getStatus().equals(Status.CANCELLED));
		}
		
		assertFalse(res);
	}
	
	// Test a resolve in protocol (Trent gives the signature in the end)
	@Test
	public void TestC(){
		resolveInitiator(2);

		
		// Time to realize procedure
		for (int k=0; k<5; k++){
			try{
				Thread.sleep(1000);
			}catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
		boolean res = true;
		for (int k=0; k<N; k++){
			res =  res && c[k].isFinalized();
		}

		assertTrue(res);
	}
	
}