package comp0012.target;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * test simple folding
 */
public class SimpleFoldingTest {

    SimpleFolding sf = new SimpleFolding();
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    
    @Before
    public void setUpStreams()
    {
        System.setOut(new PrintStream(outContent));
    }
    
    @After
    public void cleanUpStreams()
    {
        System.setOut(null);
    }

    @Test
    public void testSimple(){
        sf.simple();
        assertEquals("12412\n", outContent.toString());
    }

    
    @Test
    public void testLongAddition() {
        sf.longAdd();
        assertEquals("1500\n", outContent.toString());
    }
    
    
    @Test
    public void testFloatMultiply() {
        sf.floatMul();
        assertEquals("50.0\n", outContent.toString());
    }
    
    
    @Test
    public void testFloatDivision() {
        sf.floatDiv();
        assertEquals("2.0\n", outContent.toString());
    }
    
    @Test
    public void testDoubleDivision() {
        sf.doubleDiv();
        assertEquals("0.2\n", outContent.toString());
    }

}
