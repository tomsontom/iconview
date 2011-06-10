/*******************************************************************************
 * Copyright (c) 2010 BestSolution.at and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tom Schindl <tom.schindl@bestsolution.at> - initial API and implementation
 ******************************************************************************/
package at.bestsolution.bridge.iconview.internal;

import org.eclipse.nebula.widgets.gallery.AbstractGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.RendererHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;


public class CustomGalleryItemRenderer extends
		AbstractGalleryItemRenderer {
	@Override
	public void draw(GC gc, GalleryItem item, int index, int x, int y,
			int width, int height) {
		Image image = item.getImage();
		gc.setFont(item.getFont());
		
		int padding = 5;
		
		int maxWidth = width - padding * 2;
		int maxHeight = height - padding * 2;
		
		if( isSelected() ) {
			Color bg = gc.getBackground();
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
			gc.fillRoundRectangle(x, y, width, height, 5, 5);
			gc.setBackground(bg);
		}
		
		int maxImageHeight = maxHeight - padding -  gc.getFontMetrics().getHeight();
		
		String text = RendererHelper.createLabel(item.getText(), gc, maxWidth);
		
		int imageWidth = image.getBounds().width;
		int imageHeight = image.getBounds().height;
		
		Point imageSize;
		
		if( imageWidth <= maxWidth && imageHeight <= maxImageHeight ) {
			imageSize = new Point(imageWidth, imageHeight);
		} else {
			imageSize = RendererHelper.getBestSize(imageWidth, imageHeight, maxWidth, maxImageHeight);				
		}

		int imageX = x + padding;
		int imageY = y + padding;
		
		if( imageSize.x < maxWidth ) {
			imageX = x + width / 2 - imageSize.x / 2;
		}
		
		gc.drawImage(image, imageX, imageY);
		
		int textX = x + padding;
		int textY = y + padding + imageSize.y + padding;
		Point textSize = gc.textExtent(text);
		
		if( textSize.x < maxWidth ) {
			textX = x + width / 2 - textSize.x / 2;
		}
		
		gc.drawText(text, textX, textY, true);
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
}
