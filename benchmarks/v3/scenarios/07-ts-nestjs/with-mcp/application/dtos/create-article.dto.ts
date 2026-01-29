import { IsString, IsNotEmpty, IsArray, IsOptional, MinLength, MaxLength } from 'class-validator';

/**
 * DTO for creating a new article
 */
export class CreateArticleDto {
  @IsString()
  @IsNotEmpty({ message: 'Title is required' })
  @MinLength(3, { message: 'Title must be at least 3 characters long' })
  @MaxLength(200, { message: 'Title must not exceed 200 characters' })
  title: string;

  @IsString()
  @IsNotEmpty({ message: 'Content is required' })
  @MinLength(10, { message: 'Content must be at least 10 characters long' })
  content: string;

  @IsString()
  @IsNotEmpty({ message: 'Author ID is required' })
  authorId: string;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  tags?: string[];
}
