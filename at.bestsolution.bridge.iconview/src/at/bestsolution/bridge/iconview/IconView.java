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
package at.bestsolution.bridge.iconview;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.nebula.jface.galleryviewer.GalleryTreeViewer;
import org.eclipse.nebula.widgets.gallery.NoGroupRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import at.bestsolution.bridge.iconview.internal.CustomGalleryItemRenderer;

@SuppressWarnings("restriction")
public class IconView {

	private GalleryTreeViewer viewer;
	private IContainer container;
	private IWorkspace workspace;

	private Map<IResource, Image> imageMap = new HashMap<IResource, Image>();

	@Inject
	public IconView(IWorkspace workspace) {
		this.workspace = workspace;
		this.workspace.addResourceChangeListener(resourceListener);
	}
	
	@PostConstruct
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		viewer = new GalleryTreeViewer(parent, SWT.MULTI | SWT.V_SCROLL);
		viewer.getGallery().setItemRenderer(new CustomGalleryItemRenderer());

		NoGroupRenderer renderer = new NoGroupRenderer();
		renderer.setItemHeight(50);

		viewer.getGallery().setGroupRenderer(renderer);
		viewer.setContentProvider(contentProvider);

		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				if (element instanceof IFile) {
					Image image = imageMap.get(element);

					if (image == null) {
						image = loadImage((IFile) element);
						imageMap.put((IResource) element, image);
					}

					return image;
				}
				return super.getImage(element);
			}

			@Override
			public String getText(Object element) {
				if (element instanceof IFile) {
					IFile resource = (IFile) element;
					return resource.getName();
				}
				return super.getText(element);
			}
		});
	}

	@PreDestroy
	public void dispose() {
		this.workspace.removeResourceChangeListener(resourceListener);
	}
	
	@Focus
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	@Inject
	void setFolder(
			@Named(IServiceConstants.ACTIVE_SELECTION) @Optional Object selection) {
		IContainer container = null;
		if( selection instanceof IContainer ) {
			container = (IContainer) selection;	
		} else if( selection instanceof IStructuredSelection ) {
			if( ((IStructuredSelection) selection).getFirstElement() instanceof IContainer ) {
				container = (IContainer) ((IStructuredSelection) selection).getFirstElement();
			}
		} else if( selection instanceof List<?> ) {
			if( ! ((List<?>) selection).isEmpty() && ((List<?>) selection).get(0) instanceof IContainer ) {
				container = (IContainer) ((List<?>) selection).get(0);
			}
		}
		
		this.container = container;
		if( viewer != null ) {
			refreshViewer();	
		}
	}
	
	
	private IResourceChangeListener resourceListener = new IResourceChangeListener() {
		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				try {
					event.getDelta().accept(new IResourceDeltaVisitor() {
						public boolean visit(IResourceDelta delta)
								throws CoreException {
							if (delta.getKind() == IResourceDelta.ADDED) {
								handleChange(delta.getResource(), delta
										.getResource().getParent(), true);
							} else if (delta.getKind() == IResourceDelta.REMOVED) {
								handleChange(delta.getResource(), delta
										.getResource().getParent(), false);
							}
							return true;
						}

						private void handleChange(final IResource resource,
								final IContainer parent, final boolean added) {
							if (parent == container) {
								if (added) {
									isImageFile(resource);
									viewer.add(null, resource);
								} else {
									viewer.remove(resource);
									Image image = imageMap.remove(resource);
									if (image != null) {
										image.dispose();
									}
								}
							}
						}
					});
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	};

	private ITreeContentProvider contentProvider = new ITreeContentProvider() {
		private List<IResource> input = new ArrayList<IResource>();
		private String root = "Elements";

		@SuppressWarnings("unchecked")
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.input = (List<IResource>) newInput;
		}

		public void dispose() {

		}

		public boolean hasChildren(Object element) {
			return root == element;
		}

		public Object getParent(Object element) {
			return root;
		}

		public Object[] getElements(Object inputElement) {
			return new Object[] { root };
		}

		public Object[] getChildren(Object parentElement) {
			return input.toArray();
		}
	};
	
	private Image loadImage(final IFile file) {
		InputStream contents;
		try {
			contents = file.getContents();
			try {
				ImageData imageData = new ImageData(contents);
				Point size = getBestSize(imageData.width, imageData.height, 16,
						16);
				ImageData scaled = imageData.scaledTo(size.x, size.y);
				return new Image(viewer.getControl().getDisplay(), scaled);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return null;
	}

	private Point getBestSize(int originalX, int originalY, int maxX, int maxY) {
		double widthRatio = (double) originalX / (double) maxX;
		double heightRatio = (double) originalY / (double) maxY;

		double bestRatio = widthRatio > heightRatio ? widthRatio : heightRatio;

		int newWidth = (int) ((double) originalX / bestRatio);
		int newHeight = (int) ((double) originalY / bestRatio);

		return new Point(newWidth, newHeight);
	}

	
	private boolean isImageFile(IResource resource) {
		if (resource.getType() == IResource.FILE) {
			IFile file = (IFile) resource;
			String extension = file.getFileExtension();
			if (extension != null
					&& ("png".equalsIgnoreCase(extension)
							|| "jpg".equalsIgnoreCase(extension) || "gif"
								.equalsIgnoreCase(extension))) {
				return true;
			}
		}

		return false;
	}

	private void refreshViewer() {
		Map<IResource, Image> imageMap = this.imageMap;
		this.imageMap = new HashMap<IResource, Image>();

		final List<IResource> input = new ArrayList<IResource>();
		if (container != null) {
			try {
				container.accept(new IResourceVisitor() {

					public boolean visit(IResource resource)
							throws CoreException {
						if (resource.equals(IconView.this.container)) {
							return true;
						} else if (isImageFile(resource)) {
							input.add(resource);
						}

						return false;
					}
				});
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		viewer.setInput(input);

		for (Image img : imageMap.values()) {
			img.dispose();
		}
	}
}