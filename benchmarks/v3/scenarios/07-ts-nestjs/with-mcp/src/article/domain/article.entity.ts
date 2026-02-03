export interface Article {
  id: string;
  title: string;
  content: string;
  author: string;
  tags: string[];
  published: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateArticleProps {
  title: string;
  content: string;
  author: string;
  tags?: string[];
}

export interface UpdateArticleProps {
  title?: string;
  content?: string;
  tags?: string[];
  published?: boolean;
}
