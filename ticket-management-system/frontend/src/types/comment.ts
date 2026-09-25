import type { PageResponse } from './ticket';

export interface Comment {
  id: string;
  ticketId: string;
  authorId: string;
  body: string;
  createdAt: string;
}

export interface CreateCommentRequest {
  body: string;
}

export type CommentPageResponse = PageResponse<Comment>;
