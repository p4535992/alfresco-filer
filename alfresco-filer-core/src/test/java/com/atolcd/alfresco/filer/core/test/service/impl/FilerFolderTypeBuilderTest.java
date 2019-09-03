package com.atolcd.alfresco.filer.core.test.service.impl;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.atolcd.alfresco.filer.core.model.FilerFolderContext;
import com.atolcd.alfresco.filer.core.model.RepositoryNode;
import com.atolcd.alfresco.filer.core.service.FilerService;
import com.atolcd.alfresco.filer.core.service.impl.FilerFolderTypeBuilder;

// Could be executed in parallel but Mockito JUnit Jupiter extension does not correctly support parallel test execution yet
// See https://github.com/mockito/mockito/issues/1630
//@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(MockitoExtension.class)
public class FilerFolderTypeBuilderTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FilerService filerService;

  @Test
  public void getOrCreateWithContextEnabled() { //NOPMD - name: not a getter
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService,
        new FilerFolderContext(new RepositoryNode()), ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.named().with(randomUUID().toString());

    filerFolderTypeBuilder.getOrCreate();

    verify(filerService).operations();
    verify(filerService.operations()).getOrCreateFolder(any(), any(), any(), any(), any());
  }

  @Test
  public void getOrCreateWithContextDisabled() { //NOPMD - name: not a getter
    FilerFolderContext context = new FilerFolderContext(new RepositoryNode());
    context.enable(false);

    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, context, ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.getOrCreate();

    verifyZeroInteractions(filerService);
  }

  @Test
  public void getWithContextEnabled() { //NOPMD - name: not a getter
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService,
        new FilerFolderContext(new RepositoryNode()), ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.named().with(randomUUID().toString());

    filerFolderTypeBuilder.get();

    verify(filerService).operations();
    verify(filerService.operations()).getFolder(any(), any(), any());
  }

  @Test
  public void getWithContextDisabled() { //NOPMD - name: not a getter
    FilerFolderContext context = new FilerFolderContext(new RepositoryNode());
    context.enable(false);

    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, context, ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.get();

    verifyZeroInteractions(filerService);
  }

  @Test
  public void updateAndMoveWithContextEnabled() {
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService,
        new FilerFolderContext(new RepositoryNode()), ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.named().with(randomUUID().toString());

    filerFolderTypeBuilder.updateAndMove();

    verify(filerService, Mockito.times(2)).operations();
    verify(filerService.operations()).updateFileable(any(), any(), any());
    verify(filerService.operations()).updateFolder(any(), any(), any());
  }

  @Test
  public void updateAndMoveWithContextDisabled() {
    FilerFolderContext context = new FilerFolderContext(new RepositoryNode());
    context.enable(false);

    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, context, ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.updateAndMove();

    verifyZeroInteractions(filerService);
  }

  @Test
  public void addingPropertyInheritanceWithContextEnabled() {
    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService,
        new FilerFolderContext(new RepositoryNode()), ContentModel.TYPE_FOLDER);

    QName aspect = ContentModel.ASPECT_WORKING_COPY;

    filerFolderTypeBuilder.mandatoryPropertyInheritance(aspect);
    filerFolderTypeBuilder.optionalPropertyInheritance(aspect);

    // FilerFolderTypeBuilder does not provide a method to directly get context.
    // We use get() method to get a filerFolderBuilder and get context from it.
    filerFolderTypeBuilder.named().with(randomUUID().toString());
    FilerFolderContext context = filerFolderTypeBuilder.get().getContext();

    assertThat(context.getPropertyInheritance().getMandatoryAspects()).contains(aspect);
    assertThat(context.getPropertyInheritance().getOptionalAspects()).contains(aspect);
  }

  @Test
  public void addingPropertyInheritanceWithContextDisabled() {
    FilerFolderContext contextDisabled = new FilerFolderContext(new RepositoryNode());
    contextDisabled.enable(false);

    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, contextDisabled,
        ContentModel.TYPE_FOLDER);

    QName aspect = ContentModel.ASPECT_WORKING_COPY;

    filerFolderTypeBuilder.mandatoryPropertyInheritance(aspect);
    filerFolderTypeBuilder.optionalPropertyInheritance(aspect);

    // FilerFolderTypeBuilder does not provide a method to directly get context.
    // We use get() method to get a filerFolderBuilder and get context from it.
    FilerFolderContext context = filerFolderTypeBuilder.get().getContext();

    assertThat(context.getPropertyInheritance().getMandatoryAspects()).doesNotContain(aspect);
    assertThat(context.getPropertyInheritance().getOptionalAspects()).doesNotContain(aspect);
  }

  @Test
  public void clearingPropertyInheritanceWithContextEnabled() {
    QName aspect = ContentModel.ASPECT_WORKING_COPY;

    FilerFolderContext initialContext = new FilerFolderContext(new RepositoryNode());
    initialContext.getPropertyInheritance().getMandatoryAspects().add(aspect);
    initialContext.getPropertyInheritance().getOptionalAspects().add(aspect);

    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, initialContext,
        ContentModel.TYPE_FOLDER);

    filerFolderTypeBuilder.clearPropertyInheritance();

    // FilerFolderTypeBuilder does not provide a method to directly get context.
    // We use get() method to get a filerFolderBuilder and get context from it.
    filerFolderTypeBuilder.named().with(randomUUID().toString());
    FilerFolderContext context = filerFolderTypeBuilder.get().getContext();

    assertThat(context.getPropertyInheritance().getMandatoryAspects()).isEmpty();
    assertThat(context.getPropertyInheritance().getOptionalAspects()).isEmpty();
  }

  @Test
  public void clearingPropertyInheritanceWithContextDisabled() {
    QName aspect = ContentModel.ASPECT_WORKING_COPY;

    FilerFolderContext initialContext = new FilerFolderContext(new RepositoryNode());
    initialContext.getPropertyInheritance().getMandatoryAspects().add(aspect);
    initialContext.getPropertyInheritance().getOptionalAspects().add(aspect);
    initialContext.enable(false);

    FilerFolderTypeBuilder filerFolderTypeBuilder = new FilerFolderTypeBuilder(filerService, initialContext,
        ContentModel.TYPE_FOLDER);

    // FilerFolderTypeBuilder does not provide a method to directly get context.
    // We use get() method to get a filerFolderBuilder and get context from it.
    FilerFolderContext context = filerFolderTypeBuilder.get().getContext();

    assertThat(context.getPropertyInheritance().getMandatoryAspects()).contains(aspect);
    assertThat(context.getPropertyInheritance().getOptionalAspects()).contains(aspect);
  }
}