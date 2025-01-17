/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.ma2.DataType;

/** Test {@link ArrayDouble} */
public class TestArrayDouble {

  @Test
  public void testBasics() {
    int[] shape = new int[] {1, 2, 3};
    Storage<Double> store = new ArrayDouble.StorageD(new double[] {1, 2, 3, 4, 5, 6});
    ArrayDouble array = new ArrayDouble(shape, store);

    assertThat(array.get(0, 0, 0)).isEqualTo(1);
    assertThat(array.get(0, 0, 1)).isEqualTo(2);
    assertThat(array.get(0, 0, 2)).isEqualTo(3);
    assertThat(array.get(0, 1, 0)).isEqualTo(4);
    assertThat(array.get(0, 1, 1)).isEqualTo(5);
    assertThat(array.get(0, 1, 2)).isEqualTo(6);

    int count = 0;
    for (double val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }

    // Note that this does fail
    try {
      assertThat(array.get(0, 2, 2)).isEqualTo(8);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    try {
      assertThat(array.get(0, 1)).isEqualTo(4);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    double[] result = new double[3];
    array.arraycopy(1, result, 0, 3);
    assertThat(result).isEqualTo(new double[] {2, 3, 4});
  }

  @Test
  public void testNonCanonicalOrder() {
    int[] shape = new int[] {1, 2, 3};
    Storage<Double> store = new ArrayDouble.StorageD(new double[] {1, 2, 3, 4, 5, 6});
    Array<Double> array = new ArrayDouble(shape, store);
    array = Arrays.flip(array, 1);
    double[] expected = new double[] {4, 5, 6, 1, 2, 3};
    int count = 0;
    for (double val : array) {
      assertThat(val).isEqualTo(expected[count]);
      count++;
    }

    double[] result = new double[3];
    array.arraycopy(1, result, 0, 3);
    assertThat(result).isEqualTo(new double[] {5, 6, 1});
  }

  @Test
  public void testFactoryCopy() {
    int[] shape1 = new int[] {1, 2, 3};
    Array<Double> array1 = Arrays.factory(ArrayType.DOUBLE, shape1, new double[] {1, 2, 3, 4, 5, 6});
    Array<Double> array2 = Arrays.factory(ArrayType.DOUBLE, shape1, new double[] {7, 8, 9, 10, 11, 12});

    int[] shape = new int[] {2, 2, 3};
    Array<Double> array = Arrays.factoryCopy(ArrayType.DOUBLE, shape, ImmutableList.of(array1, array2));

    assertThat(array.get(0, 0, 0)).isEqualTo(1);
    assertThat(array.get(0, 0, 1)).isEqualTo(2);
    assertThat(array.get(0, 0, 2)).isEqualTo(3);
    assertThat(array.get(0, 1, 0)).isEqualTo(4);
    assertThat(array.get(0, 1, 1)).isEqualTo(5);
    assertThat(array.get(0, 1, 2)).isEqualTo(6);
    assertThat(array.get(1, 0, 0)).isEqualTo(7);
    assertThat(array.get(1, 0, 1)).isEqualTo(8);
    assertThat(array.get(1, 0, 2)).isEqualTo(9);
    assertThat(array.get(1, 1, 0)).isEqualTo(10);
    assertThat(array.get(1, 1, 1)).isEqualTo(11);
    assertThat(array.get(1, 1, 2)).isEqualTo(12);

    int count = 0;
    for (double val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }
  }

  @Test
  public void testFactoryArrays() {
    int[] shape1 = new int[] {1, 2, 3};
    Array<Double> array1 = Arrays.factory(ArrayType.DOUBLE, shape1, new double[] {1, 2, 3, 4, 5, 6});
    Array<Double> array2 = Arrays.factory(ArrayType.DOUBLE, shape1, new double[] {7, 8, 9, 10, 11, 12});

    int[] shape = new int[] {2, 2, 3};
    Array<Double> array = Arrays.factoryArrays(ArrayType.DOUBLE, shape, ImmutableList.of(array1, array2));

    assertThat(array.get(0, 0, 0)).isEqualTo(1);
    assertThat(array.get(0, 0, 1)).isEqualTo(2);
    assertThat(array.get(0, 0, 2)).isEqualTo(3);
    assertThat(array.get(0, 1, 0)).isEqualTo(4);
    assertThat(array.get(0, 1, 1)).isEqualTo(5);
    assertThat(array.get(0, 1, 2)).isEqualTo(6);
    assertThat(array.get(1, 0, 0)).isEqualTo(7);
    assertThat(array.get(1, 0, 1)).isEqualTo(8);
    assertThat(array.get(1, 0, 2)).isEqualTo(9);
    assertThat(array.get(1, 1, 0)).isEqualTo(10);
    assertThat(array.get(1, 1, 1)).isEqualTo(11);
    assertThat(array.get(1, 1, 2)).isEqualTo(12);

    int count = 0;
    for (double val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }

    try {
      assertThat(array.get(0, 2, 2)).isEqualTo(8);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    try {
      assertThat(array.get(0, 1)).isEqualTo(4);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    double[] result = new double[5];
    array.arraycopy(4, result, 0, 5);
    assertThat(result).isEqualTo(new double[] {5, 6, 7, 8, 9});
  }

}
