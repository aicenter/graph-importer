/* 
 * Copyright (C) 2017 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.graphimporter.osm;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;

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