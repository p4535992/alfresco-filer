package com.atolcd.alfresco.filer.core.service.impl;

import java.util.Collections;
import java.util.function.Consumer;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.springframework.dao.ConcurrencyFailureException;

import com.atolcd.alfresco.filer.core.model.FilerException;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerFolderService;
import com.atolcd.alfresco.filer.core.service.FilerModelService;
import com.atolcd.alfresco.filer.core.util.FilerNodeUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class FilerFolderServiceImpl implements FilerFolderService {

  private final FilerModelService filerModelService;
  private final NodeService nodeService;
  private final NodeDAO nodeDAO;

  public FilerFolderServiceImpl(final FilerModelService filerModelService, final NodeService nodeService, final NodeDAO nodeDAO) {
    this.filerModelService = filerModelService;
    this.nodeService = nodeService;
    this.nodeDAO = nodeDAO;
  }

  @Override
  public void fetchFolder(final RepositoryNode node, final Consumer<NodeRef> onGet) {
    fetchOrCreateFolderImpl(node, onGet, null);
    if (!node.getNodeRef().isPresent()) {
      throw new FilerException("Could not get filer folder: " + node);
    }
  }

  @Override
  public void fetchOrCreateFolder(final RepositoryNode node, final Consumer<NodeRef> onGet, final Consumer<NodeRef> onCreate) {
    fetchOrCreateFolderImpl(node, onGet, onCreate);
  }

  @Override
  public void updateFolder(final RepositoryNode node, final Consumer<NodeRef> onGet, final Consumer<NodeRef> onCreate) {
    if (FilerNodeUtils.isOriginal(node)) {
      afterCreateFolder(node, onCreate);
    } else {
      afterGetFolder(node.getNodeRef().get(), onGet);
    }
  }

  private void fetchOrCreateFolderImpl(final RepositoryNode node, final Consumer<NodeRef> onGet,
      final @CheckForNull Consumer<NodeRef> onCreate) {
    doGetFolder(node, onGet);
    if (onCreate != null && !node.getNodeRef().isPresent()) {
      NodeRef nodeRef = node.getParent().get();
      lockFolder(nodeRef);
      // Proceed with creation
      filerModelService.runWithoutSubscriberBehaviour(nodeRef, () -> {
        doCreateFolder(node, onCreate);
      });
    }
  }

  @Override
  public void deleteFolder(final NodeRef nodeRef) {
    // Node could be part of a hierarchy deletion, in this case it will be deleted that way
    if (!nodeService.hasAspect(nodeRef, ContentModel.ASPECT_PENDING_DELETE)) {
      // In case filerSegment is a fileable too
      filerModelService.runWithoutFileableBehaviour(nodeRef, () -> {
        nodeService.deleteNode(nodeRef);
      });
    }
  }

  @Override
  public void lockFolder(final NodeRef nodeRef) {
    Pair<Long, NodeRef> nodePair = nodeDAO.getNodePair(nodeRef);
    // Node could have been deleted in another concurrent transaction
    if (nodePair == null) {
      throw new ConcurrencyFailureException("Could not lock node. Node does not exist: " + nodeRef);
    }

    Long nodeId = nodePair.getFirst();
    filerModelService.runWithoutBehaviours(nodeRef, () -> {
      // This will effectively lock the node preventing other transactions to go further
      // They will be blocked here and when they become free, they will throw a ConcurrencyFailureException
      // which will cause a retry of the whole transaction in the RetryingTransactionHelper
      nodeDAO.updateNode(nodeId, null, null);
    }, ContentModel.ASPECT_AUDITABLE);
  }

  private void doGetFolder(final RepositoryNode node, final Consumer<NodeRef> onGet) {
    NodeRef nodeRef = null;
    try {
      nodeRef = nodeService.getChildByName(node.getParent().get(), ContentModel.ASSOC_CONTAINS, node.getName().get());
    } catch (InvalidNodeRefException e) {
      throw new ConcurrencyFailureException("Could not get node. Node does not exist: " + node.getParent(), e);
    }
    if (nodeRef != null) {
      node.setNodeRef(nodeRef);
      afterGetFolder(nodeRef, onGet);
    }
  }

  private void afterGetFolder(final NodeRef nodeRef, final Consumer<NodeRef> onGet) {
    onGet.accept(nodeRef);
  }

  private void doCreateFolder(final RepositoryNode node, final Consumer<NodeRef> onCreate) {
    QName assoc = QName.createQNameWithValidLocalName(NamespaceService.CONTENT_MODEL_1_0_URI, node.getName().get());
    // Node can have a fileable mandatory-aspect, but it is already created at the right place so there is no need to
    // trigger filer on it (FileableAspect#onAddAspect). Disable behaviour globally because nodeRef is unknown at creation time
    filerModelService.runWithoutFileableBehaviour(() -> {
      NodeRef nodeRef = nodeService.createNode(node.getParent().get(), ContentModel.ASSOC_CONTAINS, assoc, node.getType().get(),
          Collections.singletonMap(ContentModel.PROP_NAME, node.getName().get())).getChildRef();
      node.setNodeRef(nodeRef);
    });
    afterCreateFolder(node, onCreate);
  }

  /**
   * Run as System because current user may not have the update permission on the node anymore (he might not be the owner)
   * @see com.atolcd.alfresco.filer.repo.policy.FilerSegmentAspect#onAddAspect
   */
  private void afterCreateFolder(final RepositoryNode node, final Consumer<NodeRef> onCreate) {
    AuthenticationUtil.runAsSystem(() -> {
      // Do not trigger a filer action on itself i.e. the updated node (FileableAspect#OnUpdateProperties).
      // This is mainly due to property inheritance
      filerModelService.runWithoutFileableBehaviour(node.getNodeRef().get(), () -> {
        onCreate.accept(node.getNodeRef().get());
      });
      return null;
    });
  }
}
