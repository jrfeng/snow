package snow.player.radio;

import android.os.Bundle;
import android.os.Parcel;

import org.junit.Test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RadioStationTest {
    private final String id = "id_1234";
    private final String name = "test_hello";
    private final String description = "test_description";

    @Test
    public void constructor1Test() {
        RadioStation rs = new RadioStation();

        assertEquals("", rs.getId());
        assertEquals("unknown", rs.getName());
        assertEquals("", rs.getDescription());
    }

    @Test
    public void constructor2Test() {
        RadioStation rs = new RadioStation(id, name, description);

        assertEquals(id, rs.getId());
        assertEquals(name, rs.getName());
        assertEquals(description, rs.getDescription());
    }

    public void copyConstructorTest() {
        RadioStation source = new RadioStation("id", "name", "description");
        source.setExtra(new Bundle());

        RadioStation other = new RadioStation(source);

        assertEquals(source, other);
        assertNotNull(other.getExtra());
    }

    @Test
    public void equalsTest() {
        RadioStation rs1 = new RadioStation(id, name, description);
        RadioStation rs2 = new RadioStation(id, name, description);

        assertEquals(rs1, rs2);

        Bundle extra1 = new Bundle();
        extra1.putString("extra1", "value1");

        Bundle extra2 = new Bundle();
        extra2.putString("extra2", "value2");

        rs1.setExtra(extra1);
        rs2.setExtra(extra2);

        assertEquals(rs1, rs2);
    }

    @Test
    public void hashCodeTest() {
        RadioStation rs1 = new RadioStation(id, name, description);
        RadioStation rs2 = new RadioStation(id, name, description);

        assertEquals(rs1, rs2);
        assertEquals(rs1.hashCode(), rs2.hashCode());
    }

    @Test
    public void parcelableTest() {
        Parcel parcel = Parcel.obtain();

        RadioStation rs = new RadioStation(id, name, description);

        rs.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        RadioStation other = new RadioStation(parcel);

        assertEquals(rs, other);

        parcel.recycle();
    }
}
