/*
 * no copyright... do what you want with this
 */
package com.javieranton;

import com.codename1.io.Log;
import static com.codename1.io.Log.ERROR;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import static com.codename1.ui.CN.getCurrentForm;
import static com.codename1.ui.Component.CENTER;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;
import static com.codename1.ui.plaf.Style.UNIT_TYPE_PIXELS;
import com.codename1.ui.plaf.UIManager;
import com.codename1.util.OnComplete;
import java.io.ByteArrayOutputStream;

/**
 *
 * @author Javier Anton
 */
public class PhotoCropper {
    public static void cropImage(final Image img, int minSize, int destWidth, int destHeight, OnComplete<Image> s, String formTitle, String minimumImageSizeWarningMsg) 
        {
            Form previous = getCurrentForm();
            try
            {
                CN.lockOrientation(true);
                int imgHeight = img.getHeight();
                int imgWidth = img.getWidth();
                if(imgHeight < minSize || imgWidth < minSize)
                {
                    Dialog.show("Info", minimumImageSizeWarningMsg, "OK", "Cancel");
                    return;
                }
                
                int toolbarHeight = previous.getToolbar().getHeight();
                Form cropForm = new Form(formTitle, new BorderLayout());
                
                int pointerPressedX[]={0};
                int pointerPressedY[]={0};
                boolean[] pressedRight = {false};
                boolean[] pressedLeft = {false};
                boolean[] pressedTop = {false};
                boolean[] pressedBottom = {false};
                
                FlowLayout centerLayout = new FlowLayout(CENTER);
                centerLayout.setValign(CENTER);
                //this makes the image not overlap out of device vertically
                int[] contentPaneMargin = {0};
                int[] draggerFinalMarginY = {0};
                int[] draggerFinalMarginX = {0};
                int[] draggerFinalMarginDraggingY = {0};
                int[] draggerFinalMarginDraggingX = {0};
                Container[] boxCenter = {new Container(centerLayout)};
                boxCenter[0].getAllStyles().setMarginUnit(UNIT_TYPE_PIXELS);
                boxCenter[0].getAllStyles().setMargin(contentPaneMargin[0],contentPaneMargin[0],0,0);
                
                int[] newWidth = {cropForm.getContentPane().getWidth()};
                
                int[] newHeight = {imgHeight*newWidth[0]/imgWidth};
                //if new height is bigger than available, resize from widht
                if(newHeight[0] > Display.getInstance().getDisplayHeight() - toolbarHeight)
                {
                    newHeight[0] = Display.getInstance().getDisplayHeight() - toolbarHeight;
                    newWidth[0] = imgWidth*newHeight[0]/imgHeight;
                }
                
                boolean[] isTall = {newHeight[0] > newWidth[0]};
                int[] selectorSquareSide = {isTall[0] ? newWidth[0] : newHeight[0]};
                int[] selectorSquareSideDragging = {0};
                Container[] dragSelectorContainerOverlay = {new Container(new FlowLayout())};
                Container dragSelectorContainer = new Container(){
                    @Override 
                    public Dimension calcPreferredSize()
                    {
                        cropForm.setGlassPane((Graphics g, Rectangle rect) -> {
                            g.setColor(0x000000);
                            g.setAlpha(150); 
                            GeneralPath p = new GeneralPath(); 
                            p.setRect(new Rectangle(0, toolbarHeight, Display.getInstance().getDisplayWidth(), this.getAbsoluteY() - toolbarHeight), null);
                            GeneralPath p2 = new GeneralPath(); 
                            p2.setRect(new Rectangle(0, this.getAbsoluteY() + selectorSquareSide[0], Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight()), null);
                            GeneralPath p3 = new GeneralPath(); 
                            p3.setRect(new Rectangle(0, this.getAbsoluteY(), this.getAbsoluteX(), selectorSquareSide[0]), null);
                            GeneralPath p4 = new GeneralPath(); 
                            p4.setRect(new Rectangle(this.getAbsoluteX() + selectorSquareSide[0], this.getAbsoluteY(), Display.getInstance().getDisplayWidth() - this.getAbsoluteX() - selectorSquareSide[0], selectorSquareSide[0]), null);
                            g.fillShape(p);
                            g.fillShape(p2);
                            g.fillShape(p3);
                            g.fillShape(p4);
                            g.setAlpha(255);
                        });
                        return new Dimension(selectorSquareSide[0],selectorSquareSide[0]);
                    }
                };
                byte[] draggerImage = decode(draggerBase64);
                dragSelectorContainer.getAllStyles().setBgImage(Image.createImage(draggerImage, 0, draggerImage.length));
                dragSelectorContainer.getAllStyles().setMarginUnit(UNIT_TYPE_PIXELS);
                dragSelectorContainer.getAllStyles().setPadding(0,0,0,0);
                dragSelectorContainer.getAllStyles().setMargin(0,0,0,0);
                dragSelectorContainerOverlay[0].addComponent(dragSelectorContainer);
                
                Label imageContainer = new Label(){
                    @Override 
                    public void pointerReleased(int x, int y)
                    {
                            super.pointerReleased(x, y);
                            if((!pressedLeft[0] &&!pressedRight[0] && !pressedTop[0] && !pressedBottom[0]) || pressedTop[0])//dragging OR Top(any)
                            {
                                //reset vert
                                if(draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0] < 0)
                                    draggerFinalMarginY[0] = 0;
                                else if(draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0] >= super.getHeight())
                                    draggerFinalMarginY[0] = super.getHeight() - selectorSquareSide[0];
                                else
                                    draggerFinalMarginY[0] = draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0];
                                //reset horiz
                                if(draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0] < 0)//if dragged out on left or reached left
                                    draggerFinalMarginX[0] = 0;
                                else if(draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0] >= newWidth[0])//if dragged out on right or reached right
                                    draggerFinalMarginX[0] = newWidth[0] - selectorSquareSide[0];
                                else
                                    draggerFinalMarginX[0] = draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0];
                            }
                            else if(pressedLeft[0])
                            {
                                //reset horiz
                                if(draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0] < 0)//if dragged out on left or reached left
                                    draggerFinalMarginX[0] = 0;
                                else if(draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0] >= newWidth[0])//if dragged out on right or reached right
                                    draggerFinalMarginX[0] = newWidth[0] - selectorSquareSide[0];
                                else
                                    draggerFinalMarginX[0] = draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0];
                            }
                    }
                    @Override
                    public void pointerPressed(int x, int y) {
                            super.pointerPressed(x, y);	
                            pointerPressedX[0] = x;
                            draggerFinalMarginDraggingX[0] = x;
                            pointerPressedY[0] = y;
                            draggerFinalMarginDraggingY[0] = y;
                            selectorSquareSideDragging[0] = selectorSquareSide[0];
                            pressedRight[0] = false;
                            pressedLeft[0] = false;
                            pressedTop[0] = false;
                            pressedBottom[0] = false;
                            if(Math.abs(dragSelectorContainer.getAbsoluteX() - x) < selectorSquareSide[0] / 10)//selectors occupy 10% of dragger box approx
                                pressedLeft[0] = true;
                            if(Math.abs(dragSelectorContainer.getAbsoluteY() - y) < selectorSquareSide[0] / 10)
                                pressedTop[0] = true;
                            if(Math.abs(dragSelectorContainer.getAbsoluteY() + selectorSquareSide[0] - y) < selectorSquareSide[0] / 10)
                                pressedBottom[0] = true;
                            if(Math.abs(dragSelectorContainer.getAbsoluteX() + selectorSquareSide[0] - x) < selectorSquareSide[0] / 10)
                                pressedRight[0] = true;
                    }
                    @Override
                    public void pointerDragged(int[] x, int[] y) {
                        super.pointerDragged(x, y);
                        if(pressedLeft[0] || pressedRight[0] || pressedTop[0] || pressedBottom[0])//resizing
                        {
                            int newSize = 0;
                            if(pressedRight[0] && pressedBottom[0])//bottom right corner
                            {
                                int newSizeRight = selectorSquareSideDragging[0] - (pointerPressedX[0] - x[0]);
                                int newSizeBottom = selectorSquareSideDragging[0] - (pointerPressedY[0] - y[0]);
                                newSize = newSizeRight > newSizeBottom ? newSizeRight : newSizeBottom;
                            }else if(pressedLeft[0] && pressedBottom[0])
                            {
                                int newSizeLeft = selectorSquareSideDragging[0] - (x[0] - pointerPressedX[0]);
                                int newSizeBottom = selectorSquareSideDragging[0] + (y[0] - pointerPressedY[0]);
                                newSize = newSizeLeft > newSizeBottom ? newSizeLeft : newSizeBottom;
                                if(newSize > newWidth[0])
                                    newSize = newWidth[0];
                                if(newSizeLeft > newSizeBottom)//drag left
                                {
                                    if(newSize >= minSize)
                                    {
                                        draggerFinalMarginDraggingX[0] = x[0] - pointerPressedX[0];//this is a negative number
                                        if(draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0] - 20 < 0)//reached left
                                        {
                                            newSize = draggerFinalMarginX[0] + selectorSquareSideDragging[0];//starting point + starting size
                                            if(newSize > newWidth[0])
                                                newSize = newWidth[0];
                                            if(newSize + draggerFinalMarginY[0] > newHeight[0])
                                                newSize = newHeight[0] - draggerFinalMarginY[0];
                                            dragSelectorContainer.getAllStyles().setMarginLeft(0);
                                            selectorSquareSide[0] = newSize;
                                            System.out.println("1reached left while changing size");
                                        }
                                        else if(draggerFinalMarginY[0] + newSize + 20 >= newHeight[0])//reached the bottom(add a bit to attach to side)
                                        {
                                            newSize = newHeight[0] - draggerFinalMarginY[0];
                                            if(newSize > newWidth[0])
                                                newSize = newWidth[0];
                                            if(newSize + draggerFinalMarginY[0] > newHeight[0])
                                                newSize = newHeight[0] - draggerFinalMarginY[0];
                                            selectorSquareSide[0] = newSize;
                                            System.out.println("2reached bottom while changing size");
                                        }
                                        else
                                        {
                                            int newMargin = draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0];
                                            if(newMargin + newSize > newWidth[0])
                                                newMargin = newWidth[0] - newSize;
                                            if(newMargin < 0)
                                                newMargin = 0;
                                            dragSelectorContainer.getAllStyles().setMarginLeft(newMargin);
                                            selectorSquareSide[0] = newSize;
                                        }
                                        System.out.println("3change size. new size: "+selectorSquareSide[0]);
                                    }
                                }else//drag bottom
                                {
                                    if(newSize >= minSize)
                                    {
                                        draggerFinalMarginDraggingX[0] = x[0] - pointerPressedX[0];//this is a negative number
                                        if(draggerFinalMarginY[0] + newSize + 20 >= newHeight[0])//reached the bottom(add a bit to attach to side)
                                        {
                                            newSize = newHeight[0] - draggerFinalMarginY[0];
                                            if(newSize > newWidth[0])
                                                newSize = newWidth[0];
                                            selectorSquareSide[0] = newSize;
                                            System.out.println("4reached bottom while changing size");
                                        }
                                        else if(draggerFinalMarginX[0] - (y[0] - pointerPressedY[0]) - 20 < 0)//reached left
                                        {
                                            newSize = draggerFinalMarginX[0] + selectorSquareSideDragging[0];//starting point + starting size
                                            if(newSize > newWidth[0])
                                                newSize = newWidth[0];
                                            dragSelectorContainer.getAllStyles().setMarginLeft(0);
                                            selectorSquareSide[0] = newSize;
                                            System.out.println("5reached left while changing size");
                                        }
                                        else
                                        {
                                            int newMargin = draggerFinalMarginX[0] - (y[0] - pointerPressedY[0]);
                                            if(newMargin + newSize > newWidth[0])
                                                newMargin = newWidth[0] - newSize;
                                            if(newMargin < 0)
                                                newMargin = 0;
                                            dragSelectorContainer.getAllStyles().setMarginLeft(newMargin);
                                            selectorSquareSide[0] = newSize;
                                        }
                                        System.out.println("6change size. new size: "+selectorSquareSide[0]);
                                    }
                                }
                            }
                            else if(pressedTop[0] && pressedLeft[0])
                            {
                                int newSizeTop = selectorSquareSideDragging[0] - (y[0] - pointerPressedY[0]);
                                int newSizeLeft = selectorSquareSideDragging[0] - (x[0] - pointerPressedX[0]);
                                newSize = newSizeLeft > newSizeTop ? newSizeLeft : newSizeTop;
                                if(newSize > newWidth[0])
                                    newSize = newWidth[0];
                                if(newSize >= minSize)
                                {
                                    if(newSizeLeft > newSizeTop)//drag left
                                    {
                                            draggerFinalMarginDraggingX[0] = x[0] - pointerPressedX[0];
                                            draggerFinalMarginDraggingY[0] = y[0] - pointerPressedY[0];
                                            if(draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0] - 20 < 0)//reached left
                                            {
                                                newSize = draggerFinalMarginX[0] + selectorSquareSideDragging[0];
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                if(newSize > newHeight[0])
                                                    newSize = newHeight[0];
                                                if((draggerFinalMarginY[0] + draggerFinalMarginDraggingX[0]) + newSize > newHeight[0])
                                                {
                                                    int newMargin = newHeight[0] - newSize;
                                                    if(newMargin < 0)
                                                        newMargin = 0;
                                                    dragSelectorContainer.getAllStyles().setMarginTop(newMargin);//prevent size increase when maxing width from bottom
                                                    dragSelectorContainer.getAllStyles().setMarginLeft(0);
                                                }
                                                else
                                                {
                                                    int newMargin = draggerFinalMarginY[0] + draggerFinalMarginDraggingX[0];
                                                    if(newMargin < 0)
                                                        newMargin = 0;
                                                    if(newMargin + newSize > newHeight[0])
                                                        newMargin = 0;
                                                    dragSelectorContainer.getAllStyles().setMarginTop(newMargin);
                                                    dragSelectorContainer.getAllStyles().setMarginLeft(0);
                                                }
                                                selectorSquareSide[0] = newSize;
                                                System.out.println("24reached right while changing size ");
                                            }
                                            else if(draggerFinalMarginY[0] + draggerFinalMarginDraggingX[0] - 20 < 0)//reached the top(add a bit to attach to side)
                                            {
                                                newSize = draggerFinalMarginY[0] + selectorSquareSideDragging[0];//starting point + starting size
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                if(newSize > newHeight[0])
                                                    newSize = newHeight[0];
                                                dragSelectorContainer.getAllStyles().setMarginTop(0);
                                                int newLeftMargin = draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0];
                                                if(newLeftMargin + newSize > newWidth[0])
                                                    newLeftMargin = newWidth[0] - newSize;
                                                if(newLeftMargin < 0)
                                                    newLeftMargin = 0;
                                                dragSelectorContainer.getAllStyles().setMarginLeft(newLeftMargin);
                                                selectorSquareSide[0] = newSize;
                                                System.out.println("25reached top while changing size");
                                            }
                                            else
                                            {
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                if(newSize > newHeight[0])
                                                    newSize = newHeight[0];
                                                int newTopMargin = draggerFinalMarginY[0] + draggerFinalMarginDraggingX[0];
                                                if(newTopMargin + newSize > newHeight[0])
                                                    newTopMargin = newHeight[0] - newSize;
                                                if(newTopMargin < 0)
                                                    newTopMargin = 0;
                                                dragSelectorContainer.getAllStyles().setMarginTop(newTopMargin);
                                                
                                                int newLeftMargin = draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0];
                                                if(newLeftMargin + newSize > newWidth[0])
                                                    newLeftMargin = newWidth[0] - newSize;
                                                if(newLeftMargin < 0)
                                                    newLeftMargin = 0;
                                                dragSelectorContainer.getAllStyles().setMarginLeft(newLeftMargin);
                                                
                                                selectorSquareSide[0] = newSize;
                                            }
                                            System.out.println("26change size. new size: "+selectorSquareSide[0]);
                                    }else//drag top
                                    {
                                            draggerFinalMarginDraggingX[0] = x[0] - pointerPressedX[0];//this is negative
                                            draggerFinalMarginDraggingY[0] = y[0] - pointerPressedY[0];
                                            if(draggerFinalMarginX[0] + draggerFinalMarginDraggingY[0] - 20 < 0)//reached left
                                            {
                                                newSize = draggerFinalMarginX[0] + selectorSquareSideDragging[0];
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                if(newSize > newHeight[0])
                                                    newSize = newHeight[0];
                                                if((draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0]) + newSize > newHeight[0])
                                                {
                                                    dragSelectorContainer.getAllStyles().setMarginTop(newHeight[0] - newSize);//prevent size increase when maxing width from bottom
                                                    dragSelectorContainer.getAllStyles().setMarginLeft(0);
                                                }
                                                else
                                                {
                                                    int newMargin = draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0];
                                                    if(newMargin < 0)
                                                        newMargin = 0;
                                                    dragSelectorContainer.getAllStyles().setMarginTop(newMargin);
                                                    dragSelectorContainer.getAllStyles().setMarginLeft(0);
                                                }
                                                selectorSquareSide[0] = newSize;
                                                System.out.println("24reached right while changing size ");
                                            }
                                            else if(draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0] - 20 < 0)//reached the top(add a bit to attach to side)
                                            {
                                                newSize = draggerFinalMarginY[0] + selectorSquareSideDragging[0];//starting point + starting size
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                if(newSize > newHeight[0])
                                                    newSize = newHeight[0];
                                                dragSelectorContainer.getAllStyles().setMarginTop(0);
                                                int newLeftMargin = draggerFinalMarginX[0] + draggerFinalMarginDraggingY[0];
                                                if(newLeftMargin + newSize > newWidth[0])
                                                    newLeftMargin = newWidth[0] - newSize;
                                                if(newLeftMargin < 0)
                                                    newLeftMargin = 0;
                                                dragSelectorContainer.getAllStyles().setMarginLeft(newLeftMargin);
                                                selectorSquareSide[0] = newSize;
                                                System.out.println("25reached top while changing size");
                                            }
                                            else
                                            {
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                if(newSize > newHeight[0])
                                                    newSize = newHeight[0];
                                                int newTopMargin = draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0];
                                                if(newTopMargin + newSize > newHeight[0])
                                                    newTopMargin = newHeight[0] - newSize;
                                                if(newTopMargin < 0)
                                                    newTopMargin = 0;
                                                dragSelectorContainer.getAllStyles().setMarginTop(newTopMargin);
                                                
                                                int newLeftMargin = draggerFinalMarginX[0] + draggerFinalMarginDraggingY[0];
                                                if(newLeftMargin + newSize > newWidth[0])
                                                    newLeftMargin = newWidth[0] - newSize;
                                                if(newLeftMargin < 0)
                                                    newLeftMargin = 0;
                                                dragSelectorContainer.getAllStyles().setMarginLeft(newLeftMargin);
                                                selectorSquareSide[0] = newSize;
                                            }
                                            System.out.println("29change size. new size: "+selectorSquareSide[0]);
                                    }
                                }
                            }
                            else if(pressedLeft[0])
                            {
                                newSize = selectorSquareSideDragging[0] - (x[0] - pointerPressedX[0]);
                                if(newSize >= minSize)
                                {
                                    draggerFinalMarginDraggingX[0] = x[0] - pointerPressedX[0];//this is negative
                                    if(draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0] - 20 < 0)//reached left
                                    {
                                        newSize = draggerFinalMarginX[0] + selectorSquareSideDragging[0];//starting point + starting size
                                        if(newSize > newWidth[0])
                                            newSize = newWidth[0];
                                        if(newSize + draggerFinalMarginY[0] > newHeight[0])
                                            newSize = newHeight[0] - draggerFinalMarginY[0];
                                        dragSelectorContainer.getAllStyles().setMarginLeft(0);
                                        selectorSquareSide[0] = newSize;
                                        System.out.println("7reached left while changing size");
                                    }
                                    else if(draggerFinalMarginY[0] + newSize + 20 >= newHeight[0])//reached the bottom(add a bit to attach to side)
                                    {
                                        newSize = newHeight[0] - draggerFinalMarginY[0];
                                        if(newSize > newWidth[0])
                                            newSize = newWidth[0];
                                        if(newSize + draggerFinalMarginY[0] > newHeight[0])
                                            newSize = newHeight[0] - draggerFinalMarginY[0];
                                        selectorSquareSide[0] = newSize;
                                        System.out.println("8reached bottom while changing size");
                                    }
                                    else
                                    {
                                        int newMargin = draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0];
                                        if(newMargin + newSize > newWidth[0])
                                            newMargin = newWidth[0] - newSize;
                                        if(newMargin < 0)
                                            newMargin = 0;
                                        if(newSize + draggerFinalMarginY[0] > newHeight[0])
                                            newSize = newHeight[0] - draggerFinalMarginY[0];
                                        dragSelectorContainer.getAllStyles().setMarginLeft(newMargin);
                                        selectorSquareSide[0] = newSize;
                                    }
                                    System.out.println("9change size. new size: "+selectorSquareSide[0]);
                                }
                            }
                            else if(pressedTop[0] && pressedRight[0])
                            {
                                int newSizeTop = selectorSquareSideDragging[0] - (y[0] - pointerPressedY[0]);
                                int newSizeRight = selectorSquareSideDragging[0] - (pointerPressedX[0] - x[0]);
                                newSize = newSizeRight > newSizeTop ? newSizeRight : newSizeTop;
                                if(newSize >= minSize)
                                {
                                    if(newSizeRight > newSizeTop)//drag right
                                    {
                                            draggerFinalMarginDraggingX[0] = x[0] - pointerPressedX[0];//this is negative
                                            draggerFinalMarginDraggingY[0] = y[0] - pointerPressedY[0];
                                            if(draggerFinalMarginX[0] + newSize + 20 >= newWidth[0])//reached the right side (add a bit to attach to side)
                                            {
                                                newSize = newWidth[0] - draggerFinalMarginX[0];
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                if((draggerFinalMarginY[0] - draggerFinalMarginDraggingX[0]) + newSize > newHeight[0])
                                                    dragSelectorContainer.getAllStyles().setMarginTop(newHeight[0] - newSize);//prevent size increase when maxing width from bottom
                                                else
                                                {
                                                    int newMargin = draggerFinalMarginY[0] - draggerFinalMarginDraggingX[0];
                                                    if(newMargin < 0)
                                                        newMargin = 0;
                                                    dragSelectorContainer.getAllStyles().setMarginTop(newMargin);
                                                }
                                                selectorSquareSide[0] = newSize;
                                                System.out.println("21reached right while changing size ");
                                            }
                                            else if(draggerFinalMarginY[0] - draggerFinalMarginDraggingX[0] - 20 < 0)//reached the top(add a bit to attach to side)
                                            {
                                                newSize = draggerFinalMarginY[0] + selectorSquareSideDragging[0];//starting point + starting size
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                dragSelectorContainer.getAllStyles().setMarginTop(0);
                                                selectorSquareSide[0] = newSize;
                                                System.out.println("19reached top while changing size");
                                            }
                                            else
                                            {
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                if(newSize > newHeight[0])
                                                    newSize = newHeight[0];
                                                int newMargin = draggerFinalMarginY[0] - draggerFinalMarginDraggingX[0];
                                                if(newSize + newMargin > newHeight[0])
                                                    newMargin = newHeight[0] - newSize;
                                                dragSelectorContainer.getAllStyles().setMarginTop(newMargin);
                                                selectorSquareSide[0] = newSize;
                                            }
                                            System.out.println("20change size. new size: "+selectorSquareSide[0]);
                                    }else//drag top
                                    {
                                            draggerFinalMarginDraggingX[0] = x[0] - pointerPressedX[0];//this is negative
                                            draggerFinalMarginDraggingY[0] = y[0] - pointerPressedY[0];
                                            if(draggerFinalMarginX[0] + newSize + 20 >= newWidth[0])//reached the right side (add a bit to attach to side)
                                            {
                                                newSize = newWidth[0] - draggerFinalMarginX[0];
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                if((draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0]) + newSize > newHeight[0])
                                                    dragSelectorContainer.getAllStyles().setMarginTop(newHeight[0] - newSize);//prevent size increase when maxing width from bottom
                                                else
                                                {
                                                    int newMargin = draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0];
                                                    if (newMargin < 0)
                                                        newMargin = 0;
                                                    dragSelectorContainer.getAllStyles().setMarginTop(newMargin);
                                                }
                                                selectorSquareSide[0] = newSize;
                                                System.out.println("21reached right while changing size ");
                                            }
                                            else if(draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0] - 20 < 0)//reached the top(add a bit to attach to side)
                                            {
                                                newSize = draggerFinalMarginY[0] + selectorSquareSideDragging[0];//starting point + starting size
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                dragSelectorContainer.getAllStyles().setMarginTop(0);
                                                selectorSquareSide[0] = newSize;
                                                System.out.println("22reached top while changing size");
                                            }
                                            else
                                            {
                                                if(newSize > newWidth[0])
                                                    newSize = newWidth[0];
                                                if(newSize > newHeight[0])
                                                    newSize = newHeight[0];
                                                int newMargin = draggerFinalMarginY[0] + (y[0] - pointerPressedY[0]);
                                                if(newSize + newMargin > newHeight[0])
                                                    newMargin = newHeight[0] - newSize;
                                                dragSelectorContainer.getAllStyles().setMarginTop(newMargin);
                                                selectorSquareSide[0] = newSize;
                                            }
                                            System.out.println("23change size. new size: "+selectorSquareSide[0]);
                                    }
                                }
                            }
                            else if(pressedTop[0])
                            {
                                newSize = selectorSquareSideDragging[0] - (y[0] - pointerPressedY[0]);
                                if(newSize >= minSize)
                                {
                                    draggerFinalMarginDraggingX[0] = x[0] - pointerPressedX[0];//this is negative
                                    draggerFinalMarginDraggingY[0] = y[0] - pointerPressedY[0];// this is negative
                                    if(draggerFinalMarginX[0] + newSize + 20 >= newWidth[0])//reached the right side (add a bit to attach to side)
                                    {
                                        newSize = newWidth[0] - draggerFinalMarginX[0];
                                        if(newSize > newWidth[0])
                                            newSize = newWidth[0];
                                        if((draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0] + newSize) > newHeight[0])
                                        {
                                            int newMargin = newHeight[0] - newSize;
                                            if(newMargin < 0)
                                                newMargin = 0;
                                            dragSelectorContainer.getAllStyles().setMarginTop(newMargin);//prevent size increase when maxing width from bottom
                                        }
                                        else
                                        {
                                            int newMargin = draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0];
                                            if(newMargin < 0)
                                                newMargin = 0;
                                            if(newMargin + newSize > newHeight[0])
                                                newMargin = newHeight[0] - newSize;
                                            dragSelectorContainer.getAllStyles().setMarginTop(newMargin);
                                        }
                                        selectorSquareSide[0] = newSize;
                                        System.out.println("15reached right while changing size ");
                                    }
                                    else if(draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0] - 20 < 0)//reached the top(add a bit to attach to side)
                                    {
                                        newSize = draggerFinalMarginY[0] + selectorSquareSideDragging[0];//starting point + starting size
                                        if(newSize > newWidth[0])
                                            newSize = newWidth[0];
                                        if(newSize > newHeight[0])
                                            newSize = newHeight[0];
                                        if(draggerFinalMarginX[0] + newSize > newWidth[0])
                                            dragSelectorContainer.getAllStyles().setMarginLeft(0);
                                        dragSelectorContainer.getAllStyles().setMarginTop(0);
                                        selectorSquareSide[0] = newSize;
                                        System.out.println("16reached top while changing size");
                                    }
                                    else
                                    {
                                        if(newSize > newWidth[0])
                                            newSize = newWidth[0];
                                        if(newSize > newHeight[0])
                                            newSize = newHeight[0];
                                        int newMargin = draggerFinalMarginY[0] + (y[0] - pointerPressedY[0]);
                                        if(newMargin < 0)
                                            newMargin = 0;
                                        if(newMargin + newSize > newHeight[0])
                                            newMargin = newHeight[0] - newSize;
                                        dragSelectorContainer.getAllStyles().setMarginTop(newMargin);
                                        selectorSquareSide[0] = newSize;
                                    }
                                    System.out.println("17change size. new size: "+selectorSquareSide[0]);
                                }
                            }
                            else if(pressedRight[0])
                                newSize = selectorSquareSideDragging[0] - (pointerPressedX[0] - x[0]);
                            else if(pressedBottom[0])
                                newSize = selectorSquareSideDragging[0] - (pointerPressedY[0] - y[0]);
                            
                            
                            System.out.println("10newSize: "+newSize);
                            System.out.println("11draggerFinalMarginY[0]: "+draggerFinalMarginY[0]);
                            
                            if(newSize >= minSize)
                            {
                                //not left
                                //not left & bottom
                                //not top
                                //not top & right
                                if(!pressedTop[0] && !(pressedTop[0] && pressedRight[0]) && !(pressedTop[0] && pressedLeft[0]) && !pressedLeft[0] && !(pressedLeft[0] && pressedBottom[0]))//todo other PRESSEDs
                                {
                                    if(draggerFinalMarginX[0] + newSize + 20 >= newWidth[0])//reached the right side (add a bit to attach to side)
                                    {
                                        selectorSquareSide[0] = newWidth[0] - draggerFinalMarginX[0];
                                        System.out.println("12reached right while changing size ");
                                    }
                                    else if(draggerFinalMarginY[0] + newSize + 20 >= newHeight[0])//reached the bottom(add a bit to attach to side)
                                    {
                                        selectorSquareSide[0] = newHeight[0] - draggerFinalMarginY[0];
                                        System.out.println("13reached bottom while changing size");
                                    }
                                    else
                                    {
                                        selectorSquareSide[0] = newSize;
                                        System.out.println("14change size. new size: "+selectorSquareSide[0]);
                                    }
                                }
                            }
                            dragSelectorContainerOverlay[0].forceRevalidate();
                        }
                        else if(!pressedLeft[0] && !pressedRight[0] && !pressedTop[0] && !pressedBottom[0])//dragging
                        {
                            //HORIZONTAL DRAGGING
                            draggerFinalMarginDraggingX[0] = x[0] - pointerPressedX[0];
                            if(draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0] + selectorSquareSide[0] >= newWidth[0])
                            {
                                draggerFinalMarginX[0] = newWidth[0] - selectorSquareSide[0];
                                pointerPressedX[0] = x[0];
                                int safeMargin = newWidth[0] - selectorSquareSide[0];//guard against negative margin
                                if(safeMargin < 0)
                                    safeMargin = 0;
                                dragSelectorContainer.getAllStyles().setMarginLeft(safeMargin);
                                System.out.println("reached right");
                            }
                            else if(draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0] < 0)
                            {
                                draggerFinalMarginX[0] = 0;
                                pointerPressedX[0] = x[0];
                                dragSelectorContainer.getAllStyles().setMarginLeft(0);
                                System.out.println("reached left");
                            }
                            else
                            {
                                dragSelectorContainer.getAllStyles().setMarginLeft(draggerFinalMarginX[0] + draggerFinalMarginDraggingX[0]);
                                System.out.println("horiz. new margin: "+(x[0] - selectorSquareSide[0]/2));
                            }
                            //VERTICAL DRAGGING
                            draggerFinalMarginDraggingY[0] = y[0] - pointerPressedY[0];
                            if(draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0] < 0)//reached top
                            {
                                draggerFinalMarginY[0] = 0;
                                pointerPressedY[0] = y[0];
                                dragSelectorContainer.getAllStyles().setMarginTop(0);
                                System.out.println("reached top");
                            }else if(draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0] + selectorSquareSide[0] >= newHeight[0])//reached bottom
                            {
                                draggerFinalMarginY[0] = newHeight[0] - selectorSquareSide[0];
                                pointerPressedY[0] = y[0];
                                int newMargin = newHeight[0] - selectorSquareSide[0];
                                if(newMargin < 0)
                                    newMargin = 0;
                                dragSelectorContainer.getAllStyles().setMarginTop(newMargin);
                                System.out.println("reached bottom");
                            }
                            else //if(isTall[0] || selectorSquareSide[0] < super.getHeight())//drag vertically
                            {
                                int newMargin = draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0];
                                if(newMargin < 0)
                                    newMargin = 0;
                                dragSelectorContainer.getAllStyles().setMarginTop(newMargin);
                                System.out.println("verti. new margin: "+(draggerFinalMarginY[0] + draggerFinalMarginDraggingY[0]));
                            }
                            dragSelectorContainerOverlay[0].forceRevalidate();
                        }
                    }
                };
                imageContainer.getAllStyles().setBgImage(img);
                imageContainer.setPreferredW(newWidth[0]);
                imageContainer.setPreferredH(newHeight[0]);
                imageContainer.getAllStyles().setMarginUnit(UNIT_TYPE_PIXELS);
                imageContainer.getAllStyles().setMargin(0,0,0,0);
                imageContainer.getAllStyles().setPadding(0,0,0,0);
                
                Container[] actualContent = {LayeredLayout.encloseIn(imageContainer, dragSelectorContainerOverlay[0])};
                boxCenter[0].add(actualContent[0]);
                
                
                cropForm.add(BorderLayout.CENTER,boxCenter[0]);
                cropForm.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_CROP, e -> {
                    CN.unlockOrientation();
                    previous.showBack();
                    try
                    {
                        int originalHeight = img.getHeight();
                        int originalWidth = img.getWidth();
                        double ratio = Double.valueOf(originalHeight)/Double.valueOf(newHeight[0]);
                        int xStart = (int)Math.floor(dragSelectorContainer.getUnselectedStyle().getMarginLeft(false)*ratio);
                        int xWidth = (int)Math.floor(selectorSquareSide[0]*ratio);
                        if(xStart + xWidth > originalWidth)
                            xWidth = originalWidth - xStart;
                        int yStart = (int)Math.floor(dragSelectorContainer.getUnselectedStyle().getMarginTop()*ratio);
                        int yHeight = (int)Math.floor(selectorSquareSide[0]*ratio);
                        if(yStart + yHeight > originalHeight)
                            yHeight = originalHeight - yStart;
                        Image subImage = img.subImage(xStart, yStart, xWidth, yHeight, true);
                        if(subImage.getWidth() > destWidth || subImage.getHeight() > destHeight)
                            subImage = subImage.scaled(destWidth, destHeight);
                        s.completed(subImage);
                    }catch(Exception ex)
                    {
                        Log.p("Error cropping image: "+ex.toString(), ERROR);
                        if(img.getHeight() > destHeight || img.getWidth() > destWidth)
                            s.completed(img.scaled(destWidth, destHeight));
                        else
                            s.completed(img);
                    }
                cropForm.getToolbar().addMaterialCommandToLeftBar("", FontImage.MATERIAL_CANCEL, ee -> {
                    CN.unlockOrientation();
                    previous.showBack();
                        });
                });
                
                cropForm.show();
            }catch(Exception e)
            {
                Log.p("Error cropping image: "+e.toString(), ERROR);
                CN.unlockOrientation();
                previous.showBack();
                if(img.getHeight() > destHeight || img.getWidth() > destWidth)
                    s.completed(img.scaled(destWidth, destHeight));
                else
                    s.completed(img);
            }
    }
    public static byte[] decode(String data)
    {
        int[] tbl = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54,
            55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2,
            3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30,
            31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
            48, 49, 50, 51, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
        byte[] bytes = data.getBytes();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; ) {
            int b = 0;
            if (tbl[bytes[i]] != -1) {
                b = (tbl[bytes[i]] & 0xFF) << 18;
            }
            // skip unknown characters
            else {
                i++;
                continue;
            }

            int num = 0;
            if (i + 1 < bytes.length && tbl[bytes[i+1]] != -1) {
                b = b | ((tbl[bytes[i+1]] & 0xFF) << 12);
                num++;
            }
            if (i + 2 < bytes.length && tbl[bytes[i+2]] != -1) {
                b = b | ((tbl[bytes[i+2]] & 0xFF) << 6);
                num++;
            }
            if (i + 3 < bytes.length && tbl[bytes[i+3]] != -1) {
                b = b | (tbl[bytes[i+3]] & 0xFF);
                num++;
            }

            while (num > 0) {
                int c = (b & 0xFF0000) >> 16;
                buffer.write((char)c);
                b <<= 8;
                num--;
            }
            i += 4;
        }
        return buffer.toByteArray();
    }
    private static String draggerBase64 = "iVBORw0KGgoAAAANSUhEUgAAAyAAAAMgCAYAAADbcAZoAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAABJTSURBVHhe7d1/ahtHGIBhbW5QaPtHexadXndpodAcwduVNVHk+FecjF+2+HlAeLSxA1nC4Jdv1l5Op9N6AAAACHwaHwEAAN7dp3U1AAEAABoPjmAdj8dlLAEO9gfgLewZwHNu94dHR7BWrsYtAdiVsUWxGbcEYHfGNsVm3JIrz4AAAAAZAQIAAGReDJDlgxu3AWBXxhbFZtwSgN0Z29SHNW7Dk0xAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAJjt7/H66/7djUcBstwYlwAAAL7blhJ/jNef49KVCQgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkBAgAAJARIAAAQEaAAAAAGQECAABkltPptI41AADAuzIBAQAAMgIEAADIPDiCdTwel7EEONgfgLewZwDPud0fTEAAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADICBAAAyAgQAAAgI0AAAICMAAEAADLPBsi6rstYAgAATPEoQLbwuNte67a8u1wBAAD4fuee+GJcunpqAmLyAQAAvAvPgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEDmpQBZx0cAAIApngqQT8uF6QgAAPBmoyfujUtXjyJj+xyTDwAA4F2YcgAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAABkBAgAAZAQIAACQESAAAEBGgAAAAJnldDqtYw0AAPCuTEAAAICMAAEAADIPjmAdj8dlLAEO9gfgLewZwHNu9wcTEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMgIEAAAICNAAACAjAABAAAyAgQAAMg8CpB1XT/fvJZxGQAA4LtsHXG3ve6NS1dPTUB+uXkBAAC81bODDEewAACAzGsB4ggWAAAwjQkIAACQESAAAMA06ys/yEqAAAAAGc+AAAAAGRMQAABgJkewAACAfXAECwAAmMkEBAAA2AcBAgAAZAQIAAAw008dwfIMCAAAMI0JCAAAkBEgAADATI5gAQAA+2ACAgAAZAQIAAAwk19ECAAA7INnQAAAgIwJCAAAMJMjWAAAwD44ggUAAGRMQAAAgJkcwQIAAPbBESwAAGAmExAAAGAfBAgAAJARIAAAwEw/dQTLMyAAAMA0JiAAAEBGgAAAADM5ggUAAOyDCQgAAJARIAAAwEx+ESEAALAPngEBAAAyJiAAAMBMjmABAAD74AgWAACQMQEBAABmcgQLAADYBwECAABkPAMCAADM5AgWAACwDwIEAACY6acmII5gAQAA05iAAAAAGQECAADM5CF0AABgHzwDAgAAZExAAACAmRzBAgAA9sERLAAAIGMCAgAAzOQIFgAAsA8CBAAAyHgGBAAAmMkRLAAAYB8ECAAAkHEECwAAmOnFhlhOp9M61ofj8bism/H27NdlWf7dLn37l9y+f2599r/+s+3f/nks4UP6dn8YS4An2TPgq+3759+/LMfHs9fWb/ncs6fWP/p1Z1/WP/p15++fzznx27b853Ll/tqD5ngtQD60880aS/iQfDOxT/bpr+zT+2LPgIttmz7//7+7vOO8V9/uD54BAQAAMgIEAADmMgF8waMAOY9IuBi3BGBXxhbFZtwSgF3Ztqe7yy7F2bgtVw/OYwEAALwnR7AAAICMAAEAACKHw39R5fgaFkLunQAAAABJRU5ErkJggg==";
}
