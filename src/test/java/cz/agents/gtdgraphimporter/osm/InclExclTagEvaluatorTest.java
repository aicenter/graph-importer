package cz.agents.gtdgraphimporter.osm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Marek Cuchý
 */
public class InclExclTagEvaluatorTest {

	/**
	 * Test of fully defined evaluator
	 *
	 * @throws Exception
	 */
	@Test
	public void testTest1() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		InclExclTagEvaluator eval = mapper.readValue(this.getClass().getResourceAsStream
				("incl_excl_tag_evaluator_test1.json"), InclExclTagEvaluator.class);

		Map<String, String> t1 = new HashMap<>();

		//no tags
		assertFalse(eval.test(t1));

		//include
		t1.put("highway", "trunk");
		assertTrue(eval.test(t1));

		//exclude unless
		t1.put("access", "no");
		assertFalse(eval.test(t1));
		t1.put("motor_vehicle", "yes");
		assertTrue(eval.test(t1));

		//exclude
		t1.put("highway", "pedestrian");
		assertFalse(eval.test(t1));
	}

	/**
	 * Test if always false without included tag
	 *
	 * @throws Exception
	 */
	@Test
	public void testTest3() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		InclExclTagEvaluator eval = mapper.readValue(this.getClass().getResourceAsStream
				("incl_excl_tag_evaluator_test3.json"), InclExclTagEvaluator.class);

		Map<String, String> t1 = new HashMap<>();

		//no tags
		assertFalse(eval.test(t1));


		//exclude unless
		t1.put("access", "no");
		assertFalse(eval.test(t1));
		t1.put("motor_vehicle", "yes");
		assertFalse(eval.test(t1));

		//exclude
		t1.put("highway", "pedestrian");
		assertFalse(eval.test(t1));
	}

	/**
	 * Test of evaluator with the {@code excludeUnless} empty
	 *
	 * @throws Exception
	 */
	@Test
	public void testTest2() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		InclExclTagEvaluator eval = mapper.readValue(this.getClass().getResourceAsStream
				("incl_excl_tag_evaluator_test2.json"), InclExclTagEvaluator.class);

		Map<String, String> t1 = new HashMap<>();

		//no tags
		assertFalse(eval.test(t1));

		//include
		t1.put("highway", "trunk");
		assertTrue(eval.test(t1));

		//exclude unless
		t1.put("access", "no");
		assertTrue(eval.test(t1));
		t1.put("motor_vehicle", "yes");
		assertTrue(eval.test(t1));

		//exclude
		t1.put("highway", "pedestrian");
		assertFalse(eval.test(t1));
	}
}